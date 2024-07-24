package kr.co.saramin.lab.commutingcrw;

import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.service.SubwayService;
import kr.co.saramin.lab.commutingcrw.vo.MetroVO;
import kr.co.saramin.lab.commutingcrw.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class CommutingCrwApplication implements CommandLineRunner {

    private final Environment env;

    private final SubwayService subwayService;

    public static void main(String[] args) {
        SpringApplication.run(CommutingCrwApplication.class, args);
    }

    /**
     * 초기 매핑 사전 load
     */
    @SneakyThrows
    @PostConstruct
    public void init() {
        //  서울메트로 지하철코드 | 사람인 지하철코드
        Path path = Paths.get(Objects.requireNonNull(env.getProperty("subway.filepath")));
        Files.readAllLines(path).forEach(s -> {
            String[] cont = s.split("\\|", 6);
            Global.SRI_CODE_MAP.put(cont[4], cont[1]);
        });

        //  서울메트로 지하철코드 | 호선명
        Files.readAllLines(path).forEach(s -> {
            String[] cont = s.split("\\|", 6);
            Global.LINE_MAP.put(cont[4], cont[3]);
        });
    }

    // TODO: 2024-04-08 상세한 절차 설명 필요.

    /**
     *
     * //   전체 지하철역 정보 ( 통근 경로 구하는 지하철 코드값이 포함되어 있음. )
     * @param args incoming main method arguments
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        // TODO: 2024-07-23
        //  1. 지하철정보 관련 파일을 하나로 통합.
        //      vdi 내부망에서 기반이 되는 중복제거된 지하철역 정보파일(사람인코드도 포함된)에 통근경로를 구할때 필요한 코드를 매핑하여 파일을 하나로 관리.
        //  2.  통합 파일로 통근 경로 생성
        //      신규로 추가되는 역들 전체 매핑 -> fgf 파일로 생성

        //  통근 경로 api 테스트
//		subwayService.testCheck();
        //  지하철 정보 관련 파일 머지
        //  vdi에서 가져온 사람인 코드와 좌표 정보가 있는 지하철 파일 필요
//        subwayService.mergeMetroData();
		subwayService.getCommutingAll();
//        	metro_data 볼륨 생성
//        subwayService.makeMetroData();
//		subwayService.makeHapSubdata();
            System.exit(0);
    }
}

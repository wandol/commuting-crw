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
	 * 	초기 매핑 사전 load
	 */
	@SneakyThrows
	@PostConstruct
	public  void init(){
		//  서울메트로 지하철코드 | 사람인 지하철코드
		Path path = Paths.get(Objects.requireNonNull(env.getProperty("subway.filepath")));
		Files.readAllLines(path).forEach(s -> {
			String[] cont = s.split("\\|",6);
			Global.SRI_CODE_MAP.put(cont[4],cont[1]);
		});

		//  서울메트로 지하철코드 | 호선명
		Files.readAllLines(path).forEach(s -> {
			String[] cont = s.split("\\|",6);
			Global.LINE_MAP.put(cont[4],cont[3]);
		});
	}

	@Override
	public void run(String... args) throws Exception {
//		subwayService.testCheck();
//		subwayService.getCommutingAll();
		//	metro_data 볼륨 생성
//		subwayService.makeMetroData();
		subwayService.makeHapSubdata();
		System.exit(0);
	}
}

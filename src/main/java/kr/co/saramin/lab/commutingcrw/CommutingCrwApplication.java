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
		List<ResultVO> miss = new ArrayList<>();
		//  중복제거된 지하철역 데이터 파일 읽기
		List<MetroVO> list = Files.readAllLines(Paths.get(Objects.requireNonNull(env.getProperty("subway.dupl.filepath")))).stream()
				.map(s -> s.split("\\|", 6))
				.map(s -> MetroVO.builder()
						.code(s[0])
						.sri_code(s[1])
						.subNm(s[2])
						.line(s[3])
						.metro_code(s[4])
						.otherCd(s[5])
						.build()).collect(Collectors.toList());

		for (int i = 2; i < 3; i++) {
			List<ResultVO> resultList = new ArrayList<>();
			MetroVO startMetroVO = list.get(i);
			for (MetroVO endMetroVO : list) {
				if(!startMetroVO.getMetro_code().equals(endMetroVO.getMetro_code())){
					Thread.sleep(100);
					try {
						ResponseEntity<String> response = subwayService.getStringResponseEntity(startMetroVO, endMetroVO);
						if (response.getBody() != null && response.getStatusCodeValue() == 200) {
							Document doc = Jsoup.parse(Objects.requireNonNull(response.getBody()));
							Elements els = doc.getElementsByTag("pathList");
							if(els.size() > 0){
								Elements tels = doc.getElementsByTag("transferList");
								String stNodeNm = Global.LINE_MAP.get(Objects.requireNonNull(doc.getElementsByTag("startStationCode").first()).text());
								String stSriCd = Global.SRI_CODE_MAP.get(Objects.requireNonNull(doc.getElementsByTag("startStationCode").first()).text());
								String endCode = Global.SRI_CODE_MAP.get(Objects.requireNonNull(doc.getElementsByTag("endStationCode").first()).text());
								ResultVO vo = ResultVO.builder()
										.startNodeNmSearch(stNodeNm)
										.startStNmSearch(startMetroVO.getSubNm())
										.endCodeSearch(endCode)
										.startStNm(startMetroVO.getSubNm())
										.endStNm(endMetroVO.getSubNm())
										.pathsNm(subwayService.getPath(startMetroVO.getSubNm(), els))
										.pathsCd(subwayService.getPathCd(stSriCd, els))
										.totalCost(doc.getElementsByTag("totalTime").text())
										.transferNode(subwayService.gettransNode(stNodeNm, tels))
										.transferStNm(subwayService.gettrans(els))
										.transferStCd(subwayService.gettransCd(els))
										.build();
								resultList.add(vo);
							}else{
								ResultVO vo = ResultVO.builder()
										.startNodeNmSearch(startMetroVO.getLine())
										.startStNmSearch(startMetroVO.getSubNm())
										.endCodeSearch(endMetroVO.getSri_code())
										.startStNm(startMetroVO.getSubNm())
										.endStNm(endMetroVO.getSubNm())
										.build();
								miss.add(vo);
							}
						}else{
							ResultVO vo = ResultVO.builder()
									.startNodeNmSearch(startMetroVO.getLine())
									.startStNmSearch(startMetroVO.getSubNm())
									.endCodeSearch(endMetroVO.getSri_code())
									.startStNm(startMetroVO.getSubNm())
									.endStNm(endMetroVO.getSubNm())
									.build();
							miss.add(vo);
						}
					} catch (Exception e) {
						e.printStackTrace();
						ResultVO vo = ResultVO.builder()
								.startNodeNmSearch(startMetroVO.getLine())
								.startStNmSearch(startMetroVO.getSubNm())
								.endCodeSearch(endMetroVO.getSri_code())
								.startStNm(startMetroVO.getSubNm())
								.endStNm(endMetroVO.getSubNm())
								.build();
						miss.add(vo);
					}
				}
			}
			subwayService.filewrite(resultList,startMetroVO.getSubNm());
			subwayService.filewriteMiss(miss,startMetroVO.getSubNm());
		}
		System.exit(0);
	}
}

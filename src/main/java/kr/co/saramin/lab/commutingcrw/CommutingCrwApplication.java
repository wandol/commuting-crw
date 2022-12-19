package kr.co.saramin.lab.commutingcrw;

import kr.co.saramin.lab.commutingcrw.vo.MetroVO;
import kr.co.saramin.lab.commutingcrw.vo.ResultVO;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@Log4j2
public class CommutingCrwApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(CommutingCrwApplication.class, args);
	}

	public final static ConcurrentHashMap<String,String> SRI_CODE_MAP = new ConcurrentHashMap<>();

	public final static ConcurrentHashMap<String,String> LINE_MAP = new ConcurrentHashMap<>();

	public final static ConcurrentHashMap<String,String> ALL_MAP = new ConcurrentHashMap<>();

	@SneakyThrows
	@PostConstruct
	public  void init(){
		//  파일읽기
		Files.readAllLines(Paths.get("/Users/wandol/Downloads/temp/sri_subway_all_v4.csv")).forEach(s -> {
			String[] cont = s.split("\\|",6);
			SRI_CODE_MAP.put(cont[4],cont[1]);
		});

		//  파일읽기
		Files.readAllLines(Paths.get("/Users/wandol/Downloads/temp/sri_subway_all_v4.csv")).forEach(s -> {
			String[] cont = s.split("\\|",6);
			LINE_MAP.put(cont[4],cont[3]);
		});

		//  파일읽기
		Files.readAllLines(Paths.get("/Users/wandol/Downloads/temp/sri_subway_all_v4.csv")).forEach(s -> {
			String[] cont = s.split("\\|",6);
			ALL_MAP.put(cont[4],cont[1]);
		});
	}

	@Override
	public void run(String... args) throws Exception {
		testCheck();
		List<MetroVO> list = new ArrayList<>();
		List<ResultVO> miss = new ArrayList<>();
		//  파일읽기
		Files.readAllLines(Paths.get("/Users/wandol/Downloads/temp/depl_all_subway.csv")).forEach(s -> {
			String[] cont = s.split("\\|",6);
			MetroVO vo = MetroVO.builder()
					.code(cont[0])
					.sri_code(cont[1])
					.subNm(cont[2])
					.line(cont[3])
					.metro_code(cont[4])
					.otherCd(cont[5])
					.build();
			list.add(vo);
		});
		for (int i = 0; i < 3; i++) {
			List<ResultVO> resultList = new ArrayList<>();
			MetroVO startMetroVO = list.get(i);
			for (MetroVO endMetroVO : list) {
				if(!startMetroVO.getMetro_code().equals(endMetroVO.getMetro_code())){
					Thread.sleep(100);
					try {
						ResponseEntity<String> response = getStringResponseEntity(startMetroVO, endMetroVO);
						if (response.getBody() != null && response.getStatusCodeValue() == 200) {
							Document doc = Jsoup.parse(Objects.requireNonNull(response.getBody()));
							Elements els = doc.getElementsByTag("pathList");
							if(els.size() > 0){
								Elements tels = doc.getElementsByTag("transferList");
								ResultVO vo = ResultVO.builder()
										.startNodeNmSearch(startMetroVO.getLine())
										.startStNmSearch(startMetroVO.getSubNm())
										.endCodeSearch(endMetroVO.getSri_code())
										.startStNm(startMetroVO.getSubNm())
										.endStNm(endMetroVO.getSubNm())
										.pathsNm(getPath(startMetroVO.getSubNm(), els))
										.pathsCd(getPathCd(startMetroVO.getSri_code(), els))
										.totalCost(doc.getElementsByTag("totalTime").text())
										.transferNode(gettransNode(startMetroVO.getLine(), tels))
										.transferStNm(gettrans(els))
										.transferStCd(gettransCd(els))
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
			filewrite(resultList,startMetroVO.getSubNm());
			filewriteMiss(miss,"maiss");
		}
		System.exit(0);
	}


	@SneakyThrows
	private void testCheck() {
		URL url = new URL("http://www.seoulmetro.co.kr/kr/getRouteSearchResult.do");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
		map.add("departureId", "4703");
		map.add("arrivalId", "4704");
		map.add("sKind","1");
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String>  result = restTemplate.postForEntity(url.toURI(), request , String.class);
		log.info("result :: {}", result);

	}

	@SneakyThrows
	private static void filewriteMiss(List<ResultVO> resultList, String code) {
		Path path = Paths.get("/Users/wandol/Downloads/temp/metro_all_data_"+ code+".csv");
		try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.WRITE)){
			for (ResultVO resultVO : resultList) {
				writer.append("<__startNodeNmSearch__>").append(resultVO.getStartNodeNmSearch());
				writer.newLine();
				writer.append("<__startStNmSearch__>").append(resultVO.getStartStNmSearch());
				writer.newLine();
				writer.append("<__endCodeSearch__>").append(resultVO.getEndCodeSearch());
				writer.newLine();
				writer.append("<__startStNm__>").append(resultVO.getStartStNm());
				writer.newLine();
				writer.append("<__endStNm__>").append(resultVO.getEndStNm());
				writer.newLine();
			}
		}
	}

	@SneakyThrows
	private static void filewrite(List<ResultVO> resultList, String code) {
		Path path = Paths.get("/Users/wandol/Downloads/temp/metro_all_data_"+ code+".csv");
		try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
			for (ResultVO resultVO : resultList) {
				writer.append("<__startNodeNmSearch__>").append(resultVO.getStartNodeNmSearch());
				writer.newLine();
				writer.append("<__startStNmSearch__>").append(resultVO.getStartStNmSearch());
				writer.newLine();
				writer.append("<__endCodeSearch__>").append(resultVO.getEndCodeSearch());
				writer.newLine();
				writer.append("<__startStNm__>").append(resultVO.getStartStNm());
				writer.newLine();
				writer.append("<__endStNm__>").append(resultVO.getEndStNm());
				writer.newLine();
				writer.append("<__pathsNm__>").append(resultVO.getPathsNm());
				writer.newLine();
				writer.append("<__pathsCd__>").append(resultVO.getPathsCd());
				writer.newLine();
				writer.append("<__totalCost__>").append(resultVO.getTotalCost());
				writer.newLine();
				writer.append("<__transferNode__>").append(resultVO.getTransferNode());
				writer.newLine();
				writer.append("<__transferStNm__>").append(resultVO.getTransferStNm());
				writer.newLine();
				writer.append("<__transferStCd__>").append(resultVO.getTransferStCd());
				writer.newLine();
			}
		}
	}

	private String gettransNode(String node, Elements els) {
		LinkedList<String> set  = new LinkedList<>();
		if(els.size() > 0){
			set.add(node);
		}
		els.forEach(element -> {
			set.add(element.getElementsByTag("afterLine").text());
		});
		return String.join(",",set).replaceAll(",",",subway_transfer,");
	}

	private String gettransCd(Elements els) {
		LinkedList<String> set  = new LinkedList<>();
		els.forEach(element -> {
			if("tpath".equals(element.getElementsByTag("pathType").text())){
				set.add(ALL_MAP.getOrDefault(element.getElementsByTag("endStationCode").text(),null));
			}
		});
		return String.join(",",set);
	}

	private static String gettrans(Elements els) {
		LinkedList<String> set  = new LinkedList<>();
		els.forEach(element -> {
			if("tpath".equals(element.getElementsByTag("pathType").text())){
				set.add(element.getElementsByTag("endStationName").text());
			}
		});
		return String.join(",",set);
	}

	private String getPathCd(String sri_cd, Elements els) {
		LinkedHashSet<String> set  = new LinkedHashSet<>();
		//	초기 역 우선 add
		set.add(sri_cd);
		els.forEach(element -> {
			if("spath".equals(element.getElementsByTag("pathType").text())){
				set.add(SRI_CODE_MAP.getOrDefault(element.getElementsByTag("endStationCode").text(),null));
			}
		});
		return String.join(",",set);
	}

	private static String getPath(String subNm, Elements els) {
		LinkedHashSet<String> set  = new LinkedHashSet<>();
		//	초기 역 우선 add
		set.add(subNm);
		els.forEach(element -> {
			if("spath".equals(element.getElementsByTag("pathType").text())){
				set.add(element.getElementsByTag("endStationName").text());
			}
		});
		return String.join(",",set);
	}

	private static ResponseEntity<String> getStringResponseEntity(MetroVO startVo, MetroVO endVo) throws MalformedURLException, URISyntaxException {
		URL url = new URL("http://www.seoulmetro.co.kr/kr/getRouteSearchResult.do");
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
		map.add("departureId", startVo.getMetro_code());
		map.add("arrivalId", endVo.getMetro_code());
		map.add("sKind","1");
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		return restTemplate.postForEntity(
				url.toURI(), request , String.class);
	}
}

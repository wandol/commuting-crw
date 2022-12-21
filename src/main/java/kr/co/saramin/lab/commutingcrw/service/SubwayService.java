package kr.co.saramin.lab.commutingcrw.service;

import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.vo.MetroVO;
import kr.co.saramin.lab.commutingcrw.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubwayService {

    private final Environment env;

    @SneakyThrows
    public ResponseEntity<String> getStringResponseEntity(MetroVO startMetroVO, MetroVO endMetroVO) {
        URL url = new URL(Objects.requireNonNull(env.getProperty("api.url")));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("departureId", startMetroVO.getMetro_code());
        map.add("arrivalId", endMetroVO.getMetro_code());
        map.add("sKind","1");
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        return restTemplate.postForEntity(
                url.toURI(), request , String.class);
    }

    @SneakyThrows
    public void testCheck() {
        URL url = new URL(Objects.requireNonNull(env.getProperty("api.url")));
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
    public void filewriteMiss(List<ResultVO> resultList, String code) {
        Path path = Paths.get(String.format(Objects.requireNonNull(env.getProperty("subway.miss.filepath")),code));
        try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
            for (ResultVO resultVO : resultList) {
                writer.append(resultVO.getStartStNm()).append(",");
                writer.append(resultVO.getEndStNm());
                writer.newLine();
            }
        }
    }

    @SneakyThrows
    public void filewrite(List<ResultVO> resultList, String code) {
        Path path = Paths.get(String.format(Objects.requireNonNull(env.getProperty("subway.out.filepath")),code));
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

    public String gettransNode(String node, Elements els) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(node);
        for (Element element : els) {
            String s = element.getElementsByTag("afterLine").text();
            String s1 = "인천선".equals(s) ? "인천1호선" : s;
            set.add(s1);
        }

        return String.join(",",set).replaceAll(",",",subway_transfer,");
    }

    public String gettransCd(Elements els) {
        StringJoiner joiner = new StringJoiner(",");
        for (Element element : els) {
            if ("tpath".equals(element.getElementsByTag("pathType").text())) {
                String s = element.getElementsByTag("endStationCode").text();
                String orDefault = Global.ALL_MAP.getOrDefault(s, null);
                joiner.add(orDefault);
            }
        }
        return joiner.toString();
    }

    public String gettrans(Elements els) {
        StringJoiner joiner = new StringJoiner(",");
        for (Element element : els) {
            if ("tpath".equals(element.getElementsByTag("pathType").text())) {
                String endStationName = element.getElementsByTag("endStationName").text();
                joiner.add(endStationName);
            }
        }
        return joiner.toString();
    }

    public String getPathCd(String sri_cd, Elements els) {
        LinkedHashSet<String> set  = new LinkedHashSet<>();
        //	초기 역 우선 add
        set.add(sri_cd);
        for (Element element : els) {
            if ("spath".equals(element.getElementsByTag("pathType").text())) {
                set.add(Global.SRI_CODE_MAP.getOrDefault(element.getElementsByTag("endStationCode").text(), null));
            }
        }
        return String.join(",",set);
    }

    public String getPath(String subNm, Elements els) {
        LinkedHashSet<String> set  = new LinkedHashSet<>();
        //	초기 역 우선 add
        set.add(subNm);
        for (Element element : els) {
            if ("spath".equals(element.getElementsByTag("pathType").text())) {
                set.add(element.getElementsByTag("endStationName").text());
            }
        }
        return String.join(",",set);
    }
}

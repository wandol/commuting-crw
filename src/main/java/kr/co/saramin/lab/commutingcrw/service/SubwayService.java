package kr.co.saramin.lab.commutingcrw.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.vo.MetroDataVO;
import kr.co.saramin.lab.commutingcrw.vo.MetroVO;
import kr.co.saramin.lab.commutingcrw.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.util.stream.Stream;

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
//        map.add("arrivalId", endMetroVO.getMetro_code());
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
                String orDefault = Global.SRI_CODE_MAP.getOrDefault(s, null);
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

    @SneakyThrows
    public void getCommutingAll() {
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

//        for (int i = 2; i < 3; i++) {
            List<ResultVO> resultList = new ArrayList<>();
            MetroVO startMetroVO = list.stream().filter(metroVO -> "100150".equals(metroVO.getSri_code())).findAny().orElse(null);
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
                                String stNodeNm = Global.LINE_MAP.get(Objects.requireNonNull(doc.getElementsByTag("startStationCode").first()).text());
                                String stSriCd = Global.SRI_CODE_MAP.get(Objects.requireNonNull(doc.getElementsByTag("startStationCode").first()).text());
                                String endCode = Global.SRI_CODE_MAP.get(Objects.requireNonNull(doc.getElementsByTag("endStationCode").first()).text());
                                ResultVO vo = ResultVO.builder()
                                        .startNodeNmSearch(stNodeNm)
                                        .startStNmSearch(startMetroVO.getSubNm())
                                        .endCodeSearch(endCode)
                                        .startStNm(startMetroVO.getSubNm())
                                        .endStNm(endMetroVO.getSubNm())
                                        .pathsNm(getPath(startMetroVO.getSubNm(), els))
                                        .pathsCd(getPathCd(stSriCd, els))
                                        .totalCost(doc.getElementsByTag("totalTime").text())
                                        .transferNode(gettransNode(stNodeNm, tels))
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
            filewriteMiss(miss,startMetroVO.getSubNm());
        }
//    }

    /**
     *  지하철 좌표 데이터 기반 데이터 생성
     */
    @SneakyThrows
    public void makeMetroData() {
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
        List<MetroDataVO> resultList = new ArrayList<>();
        for (MetroVO metroVO : list) {
            ResponseEntity<String> response = getStringResponseEntity(metroVO, metroVO);
            if (response.getBody() != null && response.getStatusCodeValue() == 200) {
                Document doc = Jsoup.parse(Objects.requireNonNull(response.getBody()));
                Element latitude = Objects.requireNonNull(doc.getElementsByTag("startLatitude").first());
                Element longitude = Objects.requireNonNull(doc.getElementsByTag("startLongitude").first());
                String latitudeS = latitude.text();
                String longitudeS = longitude.text();
                if("".equals(latitude.text()) && "".equals(longitude.text())) {
                    Map<String,String> tempMap = getKakaoGps(metroVO.getSubNm() + "역");
                    latitudeS = tempMap.get("latitude");
                    longitudeS = tempMap.get("longitude");
                    log.info("not gps data : {}", metroVO.getSubNm());
                    log.info("kako find :: {}",tempMap);
                }
                MetroDataVO vo = MetroDataVO.builder()
                        .node_id(metroVO.getLine())
                        .node_nm(metroVO.getLine())
                        .st_id(metroVO.getCode())
                        .st_nm(metroVO.getSubNm())
                        .gps_x(String.format("%.5f",Double.valueOf(longitudeS.equals("")?"0":longitudeS)).replace(".",""))
                        .gps_y(String.format("%.5f",Double.valueOf(latitudeS.equals("")?"0":latitudeS)).replace(".",""))
                        .gps_x_real(longitudeS)
                        .gps_y_real(latitudeS)
                        .trans_type("subway")
                        .area_nm("metro")
                        .sri_subway_cd(metroVO.getSri_code())
                        .build();
                resultList.add(vo);
            }
        }
        Path path = Paths.get(Objects.requireNonNull(env.getProperty("metro.data.filepath")));
        try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
            for (MetroDataVO resultVO : resultList) {
                writer.append(resultVO.getNode_id()).append("|");
                writer.append(resultVO.getNode_nm()).append("|");
                writer.append(resultVO.getSt_id()).append("|");
                writer.append(resultVO.getSt_nm()).append("|");
                writer.append(resultVO.getGps_x()).append("|");
                writer.append(resultVO.getGps_y()).append("|");
                writer.append(resultVO.getGps_x_real()).append("|");
                writer.append(resultVO.getGps_y_real()).append("|");
                writer.append(resultVO.getTrans_type()).append("|");
                writer.append(resultVO.getArea_nm()).append("|");
                writer.append(resultVO.getSri_subway_cd()).append("|");
                writer.newLine();
//                writer.append("<__node_id__>").append(resultVO.getNode_id());
//                writer.newLine();
//                writer.append("<__node_nm__>").append(resultVO.getNode_nm());
//                writer.newLine();
//                writer.append("<__st_id__>").append(resultVO.getSt_id());
//                writer.newLine();
//                writer.append("<__st_nm__>").append(resultVO.getSt_nm());
//                writer.newLine();
//                writer.append("<__gps_x__>").append(resultVO.getGps_x());
//                writer.newLine();
//                writer.append("<__gps_y__>").append(resultVO.getGps_y());
//                writer.newLine();
//                writer.append("<__gps_x_real__>").append(resultVO.getGps_x_real());
//                writer.newLine();
//                writer.append("<__gps_y_real__>").append(resultVO.getGps_y_real());
//                writer.newLine();
//                writer.append("<__trans_type__>").append(resultVO.getTrans_type());
//                writer.newLine();
//                writer.append("<__area_nm__>").append(resultVO.getArea_nm());
//                writer.newLine();
//                writer.append("<__sri_subway_cd__>").append(resultVO.getSri_subway_cd());
//                writer.newLine();
            }
        }

    }

    @SneakyThrows
    private Map<String, String> getKakaoGps(String st_nm) {
        Map<String, String> resultMap = new HashMap<>();
        //API KEY
        String apiKey = "64a3f2a2e19a6652a0645e5f82215213";
        URL url = new URL(Objects.requireNonNull(env.getProperty("api.kako.url")));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization","KakaoAK " + apiKey);
        MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
        map.add("query",st_nm);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> res = restTemplate.postForEntity(url.toURI(), request , String.class);
        if (res.getBody() != null && res.getStatusCodeValue() == 200) {
            String temp = res.getBody();
            Gson  gson = new Gson();
            JsonObject object = gson.fromJson(temp, JsonObject.class);
            JsonArray arr = object.get("documents").getAsJsonArray();
            for (JsonElement jsonElement : arr) {
                if("지하철역".equals(jsonElement.getAsJsonObject().get("category_group_name").getAsString())){
                    String latitude = jsonElement.getAsJsonObject().get("y").getAsString();
                    String longitude = jsonElement.getAsJsonObject().get("x").getAsString();
                    resultMap.put("latitude",latitude);
                    resultMap.put("longitude",longitude);
                    break;
                }
            }
        }
        return resultMap;
    }

    @SneakyThrows
    public void makeHapSubdata() {
        String folderPath = env.getProperty("subway.hap.filepath");
        String folderPathTobe = env.getProperty("subway.hap-to.filepath");

        List<String> writeCon = new ArrayList<>();
        int index = 0;
        int loop = 0;
        try (Stream<Path> paths = Files.walk(Paths.get(Objects.requireNonNull(folderPath)))) {
            ArrayList<Path> list = paths.filter(Files::isRegularFile).collect(Collectors.toCollection(ArrayList::new));
            for (Path path : list) {
                index++;
                String fullpath = folderPath + path.getFileName().toString();
                List<String> temp = new ArrayList<>(Files.readAllLines(Paths.get(fullpath)));
                writeCon.addAll(temp);
                if(index % 100 == 0){
                    loop++;
                    filewriteHap(writeCon,folderPathTobe + "metro_commuting_" + loop + ".fgf");
                    writeCon.clear();
                }
                log.info("file done : {}",fullpath);
            }
        }
        if(writeCon.size() > 0 ){
            filewriteHap(writeCon,folderPathTobe + "metro_commuting_" + ++loop + ".fgf");
        }
    }

    @SneakyThrows
    private void filewriteHap(List<String> cont, String fileFullName) {
        Path path = Paths.get(fileFullName);
        try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
            for (String s : cont) {
                writer.append(s);
                writer.newLine();
            }
        }
    }
}

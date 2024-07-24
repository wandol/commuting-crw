package kr.co.saramin.lab.commutingcrw.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kr.co.saramin.lab.commutingcrw.constant.Utils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubwayService {

    private final Environment env;
    private final Utils utils;

    @SneakyThrows
    public ResponseEntity<String> getStringResponseEntity(MetroDataVO startMetroVO, MetroDataVO endMetroVO) {
        URL url = new URL(Objects.requireNonNull(env.getProperty("api.url")));
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(url.toURI())
                .queryParam("departureId", startMetroVO.getSt_id())
                .queryParam("arrivalId", endMetroVO.getSt_id())
                .queryParam("sKind", "1");
        URI uri = builder.build().toUri();
        HttpEntity<?> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                String.class);
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
                if(Global.SRI_CODE_MAP.getOrDefault(element.getElementsByTag("endStationCode").text(), null) == null){
                    log.info(element.getElementsByTag("endStationCode").text());
                    log.info("");
                }
            }
        }
        return String.join(",",set);
    }

    @SneakyThrows
    public void getCommutingAll() {
        List<ResultVO> miss = new ArrayList<>();

        //  중복제거된 지하철역 데이터 파일 읽기
        List<MetroDataVO> metroDataVOList = getSriMetroData();

//        for (int i = 2; i < 3; i++) {
        List<ResultVO> resultList = new ArrayList<>();
        MetroDataVO startMetroVO = metroDataVOList.stream().filter(metroVO -> "101101".equals(metroVO.getSri_subway_cd())).findAny().orElse(null);
        if(startMetroVO != null ){
            for (MetroDataVO endMetroVO : metroDataVOList) {
                if(!startMetroVO.getSt_id().equals(endMetroVO.getSt_id())){
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
                                        .startStNmSearch(startMetroVO.getSt_nm())
                                        .endCodeSearch(endCode)
                                        .startStNm(startMetroVO.getSt_nm())
                                        .endStNm(endMetroVO.getSt_nm())
                                        .pathsNm(getPath(startMetroVO.getSt_nm(), els))
                                        .pathsCd(getPathCd(stSriCd, els))
                                        .totalCost(doc.getElementsByTag("totalTime").text())
                                        .transferNode(gettransNode(stNodeNm, tels))
                                        .transferStNm(gettrans(els))
                                        .transferStCd(gettransCd(els))
                                        .build();
                                resultList.add(vo);
                            }else{
                                ResultVO vo = ResultVO.builder()
                                        .startNodeNmSearch(startMetroVO.getNode_nm())
                                        .startStNmSearch(startMetroVO.getSt_nm())
                                        .endCodeSearch(endMetroVO.getSri_subway_cd())
                                        .startStNm(startMetroVO.getSt_nm())
                                        .endStNm(endMetroVO.getSt_nm())
                                        .build();
                                miss.add(vo);
                            }
                        }else{
                            ResultVO vo = ResultVO.builder()
                                    .startNodeNmSearch(startMetroVO.getNode_nm())
                                    .startStNmSearch(startMetroVO.getSt_nm())
                                    .endCodeSearch(endMetroVO.getSri_subway_cd())
                                    .startStNm(startMetroVO.getSt_nm())
                                    .endStNm(endMetroVO.getSt_nm())
                                    .build();
                            miss.add(vo);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        ResultVO vo = ResultVO.builder()
                                .startNodeNmSearch(startMetroVO.getNode_nm())
                                .startStNmSearch(startMetroVO.getSt_nm())
                                .endCodeSearch(endMetroVO.getSri_subway_cd())
                                .startStNm(startMetroVO.getSt_nm())
                                .endStNm(endMetroVO.getSt_nm())
                                .build();
                        miss.add(vo);
                    }
                }
            }
        }
        filewrite(resultList,startMetroVO.getSt_nm());
        filewriteMiss(miss,startMetroVO.getSt_nm());
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

    /**
     *  내부망에서의 지하철 데이터 와 외부망에서의 지하철데이터 통합
     *  - 내부망 지하철 데이터 : metro_data.csv ( 지하철 호선,역명 및 좌표, 사람인 지하철코드 )
     *  - 외부망 지하철 데이터 : API를 활용, 통근경로를 구할때 필요한 지하철 코드값.
     *  병합하여 metro_merge_data.csv 파일 생성.
     */
    @SneakyThrows
    public void mergeMetroData() {

        //  내부 데이터
        List<MetroDataVO> metroDataVOList = getSriMetroData();
        //  외부 데이터
        List<SeoulMetroVO.Station> seoulMetroVOList = getSeoulMetroData();

        List<MetroDataVO> m = mappingCode(metroDataVOList, seoulMetroVOList);
        //  중복제거용
        String mergeDataPath = env.getProperty("metro.merge.filepath");

//        List<MetroAllDataVO> all = mappingAllCode(metroDataVOList, seoulMetroVOList);
//        //  전체
//        String allDataPath = env.getProperty("metro.all.filepath");

        utils.fileWrite(mergeDataPath, m);
//        utils.fileWrite(mergeDataPath, m);
    }

//    private List<MetroAllDataVO> mappingAllCode(List<MetroDataVO> metroDataVOList, List<SeoulMetroVO.Station> seoulMetroVOList) {
//        List<MetroAllDataVO> result = new ArrayList<>();
//        if(Objects.nonNull(metroDataVOList) && Objects.nonNull(seoulMetroVOList)){
//
//            for (SeoulMetroVO.Station station : seoulMetroVOList) {
//                MetroAllDataVO vo = MetroAllDataVO.builder()
//                        .node_id(metroDataVOList.stream().filter(metroDataVO -> station.getLineNum().equals(metroDataVO.getNode_nm())))
//                        .node_nm()
//                        .st_id(station.getStationCd())
//                        .st_nm(utils.checkStNm(station.getStationNm()))
//                        .sri_st_code(metroDataVOList.stream().filter(metroDataVO -> utils.checkStNm(station.getStationNm()).equals(metroDataVO.getSt_nm())).findFirst().get().getSri_subway_cd())
//                        .build();
//            }
//
//            Map<String, List<SeoulMetroVO.Station>> map = seoulMetroVOList.stream()
//                    .filter(Objects::nonNull)
//                    .filter(station -> station.getStationNm() != null)
//                    .collect(Collectors.groupingBy(SeoulMetroVO.Station::getStationNm));
//
//            for (MetroDataVO metroDataVO : metroDataVOList) {
//                String key = utils.checkStNm(metroDataVO.getSt_nm());
//
//                if(map.get(key) != null){
//                    metroDataVO.setSt_id(map.get(key).get(0).getStationCd());
//                }else{
//                    metroDataVO.setSt_id("reject");
//                    System.out.println("?? " + metroDataVO);
//                }
//            }
//        }
//        return metroDataVOList;
//    }

    /**
     *  사람인 내부 데이터 좌표 포함.
     * @return
     */
    @SneakyThrows
    private List<MetroDataVO> getSriMetroData() {
        String metroInFile = env.getProperty("metro.merge.filepath");
        return Files.readAllLines(Paths.get(metroInFile)).stream()
                .map(s -> s.split("\\|", 12))
                .map(ss -> MetroDataVO.builder()
                        .node_id(ss[0])
                        .node_nm(ss[1])
                        .st_id(ss[2])
                        .st_nm(ss[3])
                        .gps_x(ss[4])
                        .gps_y(ss[5])
                        .gps_x_real(ss[6])
                        .gps_y_real(ss[7])
                        .trans_type(ss[8])
                        .area_nm(ss[9])
                        .sri_subway_cd(ss[10])
                        .build())
                .collect(Collectors.toList());
    }

    /**
     *  내외부 데이터 머지.
     *  1. 지하철 역명 비교,
     *  2.  호선명 포함여부 체크
     * @param metroDataVOList
     * @param seoulMetroVOList
     * @return
     */
    private List<MetroDataVO> mappingCode(List<MetroDataVO> metroDataVOList, List<SeoulMetroVO.Station> seoulMetroVOList) {
        if(Objects.nonNull(metroDataVOList) && Objects.nonNull(seoulMetroVOList)){

            Map<String, List<SeoulMetroVO.Station>> map = seoulMetroVOList.stream()
                    .filter(Objects::nonNull)
                    .filter(station -> station.getStationNm() != null)
                    .collect(Collectors.groupingBy(SeoulMetroVO.Station::getStationNm));

            for (MetroDataVO metroDataVO : metroDataVOList) {
                String key = utils.checkStNm(metroDataVO.getSt_nm());

                if(map.get(key) != null){
                    metroDataVO.setSt_id(map.get(key).get(0).getStationCd());
                }else{
                    metroDataVO.setSt_id("reject");
                    System.out.println("?? " + metroDataVO);
                }
            }
        }
        return metroDataVOList;
    }

    /**
     *  외부 지하철 데이터 select
     * @return
     */
    @SneakyThrows
    private List<SeoulMetroVO.Station> getSeoulMetroData() {
        URL url = new URL(Objects.requireNonNull(env.getProperty("api.metro-code-url")));
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url.toURI(), String.class);
        SeoulMetroVO response = new Gson().fromJson(responseEntity.getBody(), SeoulMetroVO.class);
        return Objects.nonNull(response.getList()) ? response.getList() : null;
    }
}

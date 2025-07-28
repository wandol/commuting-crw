//package kr.co.saramin.lab.commutingcrw.module;
//
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.reflect.TypeToken;
//import kr.co.saramin.lab.commutingcrw.vo.*;
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.springframework.http.HttpEntity;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.TrustManager;
//import javax.net.ssl.X509TrustManager;
//import java.io.*;
//import java.lang.reflect.Type;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardOpenOption;
//import java.security.SecureRandom;
//import java.security.cert.X509Certificate;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class MakeRawData_BK {
//
//    private static final String RAW_DIR = "raw";
//    private final String METRO_SRI = "metro_sri.csv";
//    private final String ALL_SUBWAY = "subway.json";
//    private final String SUBWAY_TRANS = "subway_trans.csv";
//    private final String METRO_DATA = "metro_sri_coord.csv";
//    private static final String API_KEY = "64a3f2a2e19a6652a0645e5f82215213";
//    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword?query=";
//    private final Gson gson = new Gson();
//    private final RestTemplate restTemplate;
//
//    static {
//        try {
//            TrustManager[] trustAllCerts = new TrustManager[]{
//                    new X509TrustManager() {
//                        public void checkClientTrusted(X509Certificate[] xcs, String authType) {}
//                        public void checkServerTrusted(X509Certificate[] xcs, String authType) {}
//                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
//                    }
//            };
//
//            SSLContext sc = SSLContext.getInstance("TLS");
//            sc.init(null, trustAllCerts, new SecureRandom());
//            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
//
//            // 호스트네임 검증도 우회 (Jsoup에도 필요함)
//            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     *  지하철 원천 데이터 생성.
//     */
//    public void all() {
//        //  지하철 환승역 데이터 생성.
//        //  필수 파일 - metro_sri.csv  node_id|st_id|node_nm|st_nm
////        makeTransData();
//        //  좌표 데이터 구하기.
////        makeCoordinateData();
////        //  신규역일경우
////        makeNewStation();
//        //  전체 역 json 파일 내리기
////        makeSubwayAll();
//
//        //  통근
//        makeCommuting();
//        //  최종 지하철 원천 파일 확인용
////        fileCheck();
//    }
//
//    @SneakyThrows
//    public void makeSubwayAll(){
//        List<SubwayInfoVo> records;
//        Path path = Paths.get(RAW_DIR, METRO_DATA);
//        records = Files.lines(path, StandardCharsets.UTF_8)
//                .map(line -> line.split("\\|"))
//                .filter(parts -> parts.length >= 7)
//                .map(parts -> SubwayInfoVo.builder()
//                        .node_id(parts[0])
//                        .st_id(parts[1])
//                        .node_nm(parts[2])
//                        .st_nm(parts[3])
//                        .st_lon(parts[4])
//                        .st_lat(parts[5])
//                        .external_id(parts[6])
//                        .build())
//                .collect(Collectors.toList());
//        // Step 2: st_nm 기준으로 그룹핑
//        Map<String, List<SubwayInfoVo>> grouped = records.stream()
//                .collect(Collectors.groupingBy(SubwayInfoVo::getSt_nm));
//
//        // Step 3: SubwayVo 객체로 변환
//        List<SubwayVo> stationMeta = grouped.entrySet().stream()
//                .map(entry -> SubwayVo.builder()
//                        .st_nm(entry.getKey())
//                        .is_transfer(entry.getValue().size() > 1)
//                        .info(entry.getValue())
//                        .region("metro") // 필요한 경우 설정
//                        .build())
//                .collect(Collectors.toList());
//
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String json = gson.toJson(stationMeta);
//        Path outPath = Paths.get(RAW_DIR, ALL_SUBWAY);
//        Files.createDirectories(outPath.getParent()); // 디렉토리 없으면 생성
//        Files.write(outPath, json.getBytes(StandardCharsets.UTF_8));
//
//        log.info("JSON 파일 생성 완료: {}", ALL_SUBWAY);
//    }
//    /**
//     * 통근 경로 시간
//     */
//    @SneakyThrows
//    private void makeCommuting() {
////        List<SubwayVo> stationList = loadSubwayMeta(RAW_DIR + "/" + ALL_SUBWAY);
//        List<SubwayVo> stationList = loadSubwayMeta(RAW_DIR + "/subway_test.json");
//        List<SubwayInfoVo> allStations = stationList.stream()
//                .flatMap(vo -> vo.getInfo().stream())
//                .collect(Collectors.toList());
//        Map<String, SubwayInfoVo> stationMap = allStations.stream()
//                .collect(Collectors.toMap(SubwayInfoVo::getExternal_id, s -> s, (a, b) -> a));
//
//        BufferedWriter errorLog = Files.newBufferedWriter(Paths.get("./logs/failures.log"), StandardCharsets.UTF_8,
//                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
//
//        for (SubwayVo fromVo : stationList) {
//            List<CommutingAllData> results = new ArrayList<>();
//            SubwayInfoVo from = fromVo.getInfo().get(0);
//            for (SubwayVo toVo : stationList) {
//                SubwayInfoVo to = toVo.getInfo().get(0);
//                if (!from.getExternal_id().equals(to.getExternal_id())) {
//                    try {
//                        CommutingAllData result = processRoute(from, to, stationMap, "metro");
//                        if (result != null) {
//                            results.add(result);
//                        } else {
//                            errorLog.write(String.join(",", "[PROCESS FAIL]", from.getExternal_id(), to.getExternal_id()) + "\n");
//                        }
//                    } catch (Exception e) {
//                        errorLog.write(String.join(",", "[EXCEPTION]", from.getExternal_id(), to.getExternal_id(), e.getMessage()) + "\n");
//                    }
//                    errorLog.flush();
//                }
//            }
//            Path outputPath = Paths.get("routes", from.getSt_id() + "_routes.json");
//            Files.createDirectories(outputPath.getParent());
//            Files.write(outputPath, gson.toJson(results).getBytes(StandardCharsets.UTF_8));
//        }
//    }
//
//    public CommutingAllData processRoute(SubwayInfoVo from, SubwayInfoVo to, Map<String, SubwayInfoVo> stationMap, String region) {
//        try {
//            String apiUrl = "https://map.kakao.com/api/subway/routes/current/metropolitan/" + from.getExternal_id() + "--08:00:00/" + to.getExternal_id() + "?resultTimeFormat=TIME_STRING&findingType=NORMAL&dayType=WEEKDAY";
//
//            // 기존에 이미 내려진 파일이 있으면 활용
//            Path debugDir = Paths.get("route_api_responses", from.getSt_id());
//            Files.createDirectories(debugDir);
//            String fileName = from.getExternal_id() + "_TO_" + to.getExternal_id() + ".json";
//            Path responseFile = debugDir.resolve(fileName);
//
//            String json;
//            if (Files.exists(responseFile)) {
//                json = Files.readString(responseFile);
//            } else {
//                json = fetch(apiUrl);
//                Files.write(responseFile, json.getBytes(StandardCharsets.UTF_8));
//            }
//
//            Type type = new TypeToken<Map<String, Object>>() {}.getType();
//            Map<String, Object> tree = gson.fromJson(json, type);
//            @SuppressWarnings("unchecked")
//            Map<String, Object> body = (Map<String, Object>) tree.get("body");
//            @SuppressWarnings("unchecked")
//            Map<String, Object> routes = (Map<String, Object>) body.get("routes");
//            @SuppressWarnings("unchecked")
//            Map<String, Object> shortest = (Map<String, Object>) routes.get("shortest_time");
//            @SuppressWarnings("unchecked")
//            List<Map<String, Object>> sections = (List<Map<String, Object>>) shortest.get("sections");
//
//            List<SubwayInfoVo> path = new ArrayList<>();
//            List<String> transferLines = new ArrayList<>();
//            List<String> transferStNms = new ArrayList<>();
//            List<String> transferStCds = new ArrayList<>();
//
//            for (int i = 0; i < sections.size(); i++) {
//                Map<String, Object> section = sections.get(i);
//                @SuppressWarnings("unchecked")
//                List<Map<String, Object>> nodes = (List<Map<String, Object>>) section.get("nodes");
//                String lineName = (String) section.get("line_name");
//                if (i > 0) transferLines.add("subway_transfer");
//                transferLines.add(lineName);
//
//                for (Map<String, Object> node : nodes) {
//                    String sid = (String) node.get("station_id");
//                    SubwayInfoVo st = stationMap.get(sid);
//                    if (st != null) path.add(st);
//                }
//
//                if (i < sections.size() - 1) {
//                    Map<String, Object> nextSection = sections.get(i + 1);
//                    @SuppressWarnings("unchecked")
//                    List<Map<String, Object>> nextNodes = (List<Map<String, Object>>) nextSection.get("nodes");
//                    if (!nextNodes.isEmpty()) {
//                        String transferExtId = (String) nextNodes.get(0).get("station_id");
//                        SubwayInfoVo transferStation = stationMap.get(transferExtId);
//                        if (transferStation != null) {
//                            transferStNms.add(transferStation.getSt_nm());
//                            transferStCds.add(transferStation.getSt_id());
//                        }
//                    }
//                }
//            }
//
//            return CommutingAllData.builder()
//                    .from(from)
//                    .to(to)
//                    .totalCost((int) Math.ceil(((Double) shortest.get("time_required")) / 60.0))
//                    .path(path)
//                    .transferNode(String.join(",", transferLines))
//                    .transferStNm(String.join(",", transferStNms))
//                    .transferStCd(String.join(",", transferStCds))
//                    .region(region)
//                    .build();
//
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    public String fetch(String urlStr) throws IOException {
//        URL url = new URL(urlStr);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("GET");
//        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
//        try (InputStream in = conn.getInputStream();
//             InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
//             BufferedReader reader = new BufferedReader(isr)) {
//            return reader.lines().collect(Collectors.joining());
//        }
//    }
//
//    private void fileCheck() {
//        List<MetroSriVO> stationList = readCsvWithReflection(METRO_DATA,
//                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
//        for (MetroSriVO m : stationList) {
//            if(m.getNode_nm().isEmpty() && m.getExternal_id().isEmpty()
//                && m.getNode_id().isEmpty() && m.getSt_nm().isEmpty()
//            && m.getSt_id().isEmpty() && m.getSt_lon().isEmpty()
//            && m.getSt_lat().isEmpty()) {
//                log.info(m.toString());
//            }
//        }
//    }
//
//    /**
//     *  신규역일 경우 최종 파일에 하단에 콘솔로그 복사하여 붙여넣기.
//     */
//    private void makeNewStation() {
//        MetroSriVO station = new MetroSriVO();
//        station.setSt_id("지하철역id");
//        station.setNode_id("호선id");
//        station.setNode_nm("1호선");
//        station.setSt_nm("신규역");
//        Optional<KakaoApiResponse.Document> docOpt = fetchSubwayCoordinates(station);
//
//        if (docOpt.isEmpty()) {
//            System.out.println(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), "", ""));
//        } else {
//            String x = docOpt.get().x;
//            String y = docOpt.get().y;
//            String ogId = extractOgUrlId(docOpt.get().place_url).orElse("");
//            System.out.println(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), x, y, ogId));
//        }
//    }
//
//    /**
//     *  해당 역의 좌표와 통근 데이터 생성에 활용될 kakao 지하철역 id를 append
//     */
//    public void makeCoordinateData() {
//        // metro_sri.csv 읽기
//        List<MetroSriVO> stationList = readCsvWithReflection(METRO_SRI,
//                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));
//        List<String> outputLines = new ArrayList<>();
//        for (MetroSriVO station : stationList) {
//            try {
//                Optional<KakaoApiResponse.Document> docOpt = fetchSubwayCoordinates(station);
//
//                if (docOpt.isEmpty()) {
//                    outputLines.add(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), "", ""));
//                } else {
//                    String x = docOpt.get().x;
//                    String y = docOpt.get().y;
//                    String ogId = extractOgUrlId(docOpt.get().place_url).orElse("");
//
//                    outputLines.add(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), x, y, ogId));
//                    log.info("[COORD FETCH] {} → {}, {}", station.getSt_nm(), x, y);
//                }
//                Thread.sleep(200); // API rate limit
//            } catch (Exception e) {
//                log.error("[COORD FETCH] Error for station {}: {}", station.getSt_nm(), e.getMessage(), e);
//            }
//        }
//
//        writeCsv(METRO_DATA, outputLines);
//    }
//
//    /**
//     * sri 지하철 명으로 중복을 체크하여 환승역 데이터 생성
//     * output - st_nm|st_id,st_id,...
//     */
//    private void makeTransData() {
//        try {
//            List<MetroSriVO> sriData = readCsvWithReflection(METRO_SRI,
//                    parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));
//
//            Map<String, List<String>> transferData = sriData.stream()
//                    .collect(Collectors.groupingBy(
//                            MetroSriVO::getSt_nm,
//                            Collectors.mapping(MetroSriVO::getSt_id, Collectors.toList())
//                    ))
//                    .entrySet().stream()
//                    .filter(entry -> entry.getValue().size() > 1) // 환승역만 필터링
//                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
//
//            List<String> lines = transferData.entrySet().stream()
//                    .map(entry -> entry.getKey() + "|" + String.join(",", entry.getValue()))
//                    .collect(Collectors.toList());
//
//            writeCsv(SUBWAY_TRANS, lines);
//
//        } catch (Exception e) {
//            log.error("[TRANSFER GENERATION] Error in makeTransData", e);
//        }
//    }
//
//    /**
//     * CSV 파일 읽기 (resources 하위), 라인 → 객체 매핑 함수 전달
//     */
//    private <T> List<T> readCsvWithReflection(String fileName, Function<String[], T> mapper) {
//        try {
//            Path path = Paths.get(RAW_DIR, fileName);
//            try (Stream<String> lines = Files.lines(path)) {
//                return lines.map(line -> line.split("\\|"))
//                        .map(mapper)
//                        .collect(Collectors.toList());
//            }
//        } catch (Exception e) {
//            log.error("Failed to load file {}: {}", fileName, e.getMessage(), e);
//            return Collections.emptyList();
//        }
//    }
//
//    /**
//     * 리스트를 CSV 파일로 쓰기 (루트 기준)
//     */
//    private void writeCsv(String fileName, List<String> lines) {
//        try {
//            Path outputPath = Paths.get(RAW_DIR, fileName);
//            Files.createDirectories(outputPath.getParent());
//            Files.write(outputPath, lines);
//            log.info("[TRANSFER GENERATION] Written {} lines to {}", lines.size(), outputPath);
//        } catch (IOException e) {
//            log.error("Failed to write file {}: {}", fileName, e.getMessage(), e);
//        }
//    }
//
//    /**
//     *  지하철 좌표 생성.
//     */
//    private Optional<KakaoApiResponse.Document> fetchSubwayCoordinates(MetroSriVO station) {
//        try {
//            String query = station.getNode_nm() + " " + station.getSt_nm() + "역";
//            String url = BASE_URL + query;
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization", "KakaoAK " + API_KEY);
//
//            HttpEntity<Void> entity = new HttpEntity<>(headers);
//            ResponseEntity<String> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.GET,
//                    entity,
//                    String.class
//            );
//            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
//                log.warn("[COORD FETCH] No response for station: {}", station.getSt_nm());
//                return Optional.empty();
//            }
//            // JSON → 객체 변환 (Gson)
//            KakaoApiResponse result = gson.fromJson(response.getBody(), KakaoApiResponse.class);
//
//            return result.documents.stream()
//                    .filter(doc -> "지하철역".equals(doc.category_group_name))
//                    .findFirst();
//
//        } catch (Exception e) {
//            log.error("[COORD FETCH] Error fetching coords for {}: {}", station.getSt_nm(), e.getMessage(), e);
//            return Optional.empty();
//        }
//    }
//
//    /**
//     *  kakao 지하철역 id  추출
//     */
//    private Optional<String> extractOgUrlId(String placeUrl) {
//        try {
//            Document doc = Jsoup.connect(placeUrl)
//                    .userAgent("Mozilla/5.0")
//                    .timeout(5000)
//                    .get();
//
//            Element og = doc.selectFirst("meta[property=og:url]");
//            if (og != null) {
//                String content = og.attr("content");
//                return Optional.of(content.replace("https://place.map.kakao.com/", ""));
//            }
//        } catch (Exception e) {
//            log.warn("[OG URL] Failed to parse og:url from {}: {}", placeUrl, e.getMessage());
//        }
//        return Optional.empty();
//    }
//
//    /**
//     *  지하철 역 전체 데이터 load
//     */
//    public List<SubwayVo> loadSubwayMeta(String filename) throws IOException {
//        try (Reader reader = new FileReader(filename, StandardCharsets.UTF_8)) {
//            SubwayVo[] list = gson.fromJson(reader, SubwayVo[].class);
//            return Arrays.asList(list);
//        }
//    }
//
//}
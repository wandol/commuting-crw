package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import kr.co.saramin.lab.commutingcrw.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MakeRawData {

    private static final String RAW_DIR = "raw";
    private final String METRO_SRI = "metro_sri.csv";
    private final String SUBWAY_TRANS = "subway_trans.csv";
    private final String METRO_DATA = "metro_sri_coord.csv";
    private static final String API_KEY = "64a3f2a2e19a6652a0645e5f82215213";
    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword?query=";
    private final Gson gson = new Gson();
    private final RestTemplate restTemplate;

    static {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public void checkClientTrusted(X509Certificate[] xcs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] xcs, String authType) {}
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // 호스트네임 검증도 우회 (Jsoup에도 필요함)
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *  지하철 원천 데이터 생성.
     */
    public void all() {
        //  지하철 환승역 데이터 생성.
        //  필수 파일 - metro_sri.csv  node_id|st_id|node_nm|st_nm
//        makeTransData();
        //  좌표 데이터 구하기.
//        makeCoordinateData();
//        //  신규역일경우
//        makeNewStation();
        //  통근
        makeCommuting();
        //  최종 지하철 원천 파일 확인용
//        fileCheck();
    }

    /**
     * 통근 경로 시간
     */
    @SneakyThrows
    private void makeCommuting() {
        List<MetroSriVO> stationList = readCsvWithReflection(METRO_DATA,
                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
//        Map<String, MetroSriVO> stationMap = stationList.stream()
//                .collect(Collectors.toMap(MetroSriVO::getExternal_id, st -> st));

        Map<String, List<MetroSriVO>> grouped = stationList.stream()
                .collect(Collectors.groupingBy(MetroSriVO::getExternal_id));

// 중복된 external_id만 필터링
        grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .forEach(entry -> {
                    System.out.println("⚠️ Duplicate external_id: " + entry.getKey());
                    entry.getValue().forEach(vo ->
                            System.out.printf("   ↳ nodeId=%s, code=%s, name=%s, line=%s%n",
                                    vo.getNode_id(), vo.getSt_id(), vo.getSt_nm(), vo.getNode_nm()));
                });

//        for (MetroSriVO from : stationList) {
//            List<CommutingAllData> results = new ArrayList<>();
//            for (MetroSriVO to : stationList) {
//                if (from.getExternal_id().equals(to.getExternal_id())) continue;
//                CommutingAllData result = processRoute(from, to, stationMap);
//                if (result != null) {
//                    results.add(result);
//                } else {
//                    log.error("{}/{}", from.getExternal_id(), to.getExternal_id());
//                    System.exit(1);
//                }
//            }
//            Path outputPath = Paths.get("routes", from.getSt_id() + "_routes.json");
//            Files.createDirectories(outputPath.getParent());
//            Files.write(outputPath, gson.toJson(results).getBytes(StandardCharsets.UTF_8));
//        }
    }

    public CommutingAllData processRoute(MetroSriVO from, MetroSriVO to, Map<String, MetroSriVO> stationMap) {
        try {
            String apiUrl = "https://map.kakao.com/api/subway/routes/current/metropolitan/" + from.getExternal_id() + "--08:00:00/" + to.getExternal_id() + "?resultTimeFormat=TIME_STRING&findingType=NORMAL&dayType=WEEKDAY";
            String json = fetch(apiUrl);
            Map<String, Object> tree = gson.fromJson(json, Map.class);
            Map<String, Object> body = (Map<String, Object>) tree.get("body");
            Map<String, Object> routes = (Map<String, Object>) body.get("routes");
            Map<String, Object> shortest = (Map<String, Object>) routes.get("shortest_time");
            List<Map<String, Object>> sections = (List<Map<String, Object>>) shortest.get("sections");

            List<Map<String, String>> externalRoute = new ArrayList<>();
            List<String> pathsNm = new ArrayList<>();
            List<String> pathsCd = new ArrayList<>();
            List<String> transferLines = new ArrayList<>();
            List<String> transferStNms = new ArrayList<>();
            List<String> transferStCds = new ArrayList<>();

            for (int i = 0; i < sections.size(); i++) {
                Map<String, Object> section = sections.get(i);
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) section.get("nodes");
                String lineName = (String) section.get("line_name");
                if (i > 0) transferLines.add("subway_transfer");
                transferLines.add(lineName);

                for (Map<String, Object> node : nodes) {
                    String sid = (String) node.get("station_id");
                    externalRoute.add(Map.of(
                            "station_id", sid,
                            "station_name", (String) node.get("station_name")
                    ));
                    MetroSriVO st = stationMap.get(sid);
                    if (st != null) {
                        pathsNm.add(st.getSt_nm());
                        pathsCd.add(st.getSt_id());
                    }
                }

                Map<String, Object> lastNode = nodes.get(nodes.size() - 1);
                String transferExtId = (String) lastNode.get("station_id");
                MetroSriVO transferStation = stationMap.get(transferExtId);
                if (transferStation != null) {
                    transferStNms.add(transferStation.getSt_nm());
                    transferStCds.add(transferStation.getSt_id());
                }
            }

            CommutingAllData output = new CommutingAllData();
            output.setFromSt(from.getSt_nm());
            output.setFromId(from.getSt_id());
            output.setFromNodeNm(from.getNode_nm());
            output.setFromNodeId(from.getNode_id());
            output.setTo(to.getSt_nm());
            output.setToId(to.getSt_id());
            output.setToNodeNm(to.getNode_nm());
            output.setToNodeId(to.getNode_id());
            output.setX_coordinate(from.getSt_lat());
            output.setY_coordinate(from.getSt_lon());
            output.setTotalCost((int) Math.ceil(((Double) shortest.get("time_required")) / 60.0));
            output.setPathsNm(pathsNm);
            output.setPathsCd(pathsCd);
            output.setTransferNode(String.join(",", transferLines));
            output.setTransferStNm(String.join(",", transferStNms));
            output.setTransferStCd(String.join(",", transferStCds));
            output.setExternal_route(externalRoute);

            return output;
        } catch (Exception e) {
            System.err.println("[ERROR] " + from.getSt_nm() + " → " + to.getSt_nm() + ": " + e.getMessage());
            return null;
        }
    }

    public String fetch(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (InputStream in = conn.getInputStream();
             InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private void fileCheck() {
        List<MetroSriVO> stationList = readCsvWithReflection(METRO_DATA,
                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));
        for (MetroSriVO m : stationList) {
            if(m.getNode_nm().isEmpty() && m.getExternal_id().isEmpty()
                && m.getNode_id().isEmpty() && m.getSt_nm().isEmpty()
            && m.getSt_id().isEmpty() && m.getSt_lon().isEmpty()
            && m.getSt_lat().isEmpty()) {
                log.info(m.toString());
            }
        }
    }

    /**
     *  신규역일 경우 최종 파일에 하단에 콘솔로그 복사하여 붙여넣기.
     */
    private void makeNewStation() {
        MetroSriVO station = new MetroSriVO();
        station.setSt_id("지하철역id");
        station.setNode_id("호선id");
        station.setNode_nm("1호선");
        station.setSt_nm("신규역");
        Optional<KakaoApiResponse.Document> docOpt = fetchSubwayCoordinates(station);

        if (docOpt.isEmpty()) {
            System.out.println(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), "", ""));
        } else {
            String x = docOpt.get().x;
            String y = docOpt.get().y;
            String ogId = extractOgUrlId(docOpt.get().place_url).orElse("");
            System.out.println(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), x, y, ogId));
        }
    }

    /**
     *  해당 역의 좌표와 통근 데이터 생성에 활용될 kakao 지하철역 id를 append
     */
    public void makeCoordinateData() {
        // metro_sri.csv 읽기
        List<MetroSriVO> stationList = readCsvWithReflection(METRO_SRI,
                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));
        List<String> outputLines = new ArrayList<>();
        for (MetroSriVO station : stationList) {
            try {
                Optional<KakaoApiResponse.Document> docOpt = fetchSubwayCoordinates(station);

                if (docOpt.isEmpty()) {
                    outputLines.add(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), "", ""));
                } else {
                    String x = docOpt.get().x;
                    String y = docOpt.get().y;
                    String ogId = extractOgUrlId(docOpt.get().place_url).orElse("");

                    outputLines.add(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), x, y, ogId));
                    log.info("[COORD FETCH] {} → {}, {}", station.getSt_nm(), x, y);
                }
                Thread.sleep(200); // API rate limit
            } catch (Exception e) {
                log.error("[COORD FETCH] Error for station {}: {}", station.getSt_nm(), e.getMessage(), e);
            }
        }

        writeCsv(METRO_DATA, outputLines);
    }

    /**
     * sri 지하철 명으로 중복을 체크하여 환승역 데이터 생성
     * output - st_nm|st_id,st_id,...
     */
    private void makeTransData() {
        try {
            List<MetroSriVO> sriData = readCsvWithReflection(METRO_SRI,
                    parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));

            Map<String, List<String>> transferData = sriData.stream()
                    .collect(Collectors.groupingBy(
                            MetroSriVO::getSt_nm,
                            Collectors.mapping(MetroSriVO::getSt_id, Collectors.toList())
                    ))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1) // 환승역만 필터링
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<String> lines = transferData.entrySet().stream()
                    .map(entry -> entry.getKey() + "|" + String.join(",", entry.getValue()))
                    .collect(Collectors.toList());

            writeCsv(SUBWAY_TRANS, lines);

        } catch (Exception e) {
            log.error("[TRANSFER GENERATION] Error in makeTransData", e);
        }
    }

    /**
     * CSV 파일 읽기 (resources 하위), 라인 → 객체 매핑 함수 전달
     */
    private <T> List<T> readCsvWithReflection(String fileName, Function<String[], T> mapper) {
        try {
            Path path = Paths.get(RAW_DIR, fileName);
            try (Stream<String> lines = Files.lines(path)) {
                return lines.map(line -> line.split("\\|"))
                        .map(mapper)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to load file {}: {}", fileName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 리스트를 CSV 파일로 쓰기 (루트 기준)
     */
    private void writeCsv(String fileName, List<String> lines) {
        try {
            Path outputPath = Paths.get(RAW_DIR, fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, lines);
            log.info("[TRANSFER GENERATION] Written {} lines to {}", lines.size(), outputPath);
        } catch (IOException e) {
            log.error("Failed to write file {}: {}", fileName, e.getMessage(), e);
        }
    }

    /**
     *  지하철 좌표 생성.
     */
    private Optional<KakaoApiResponse.Document> fetchSubwayCoordinates(MetroSriVO station) {
        try {
            String query = station.getNode_nm() + " " + station.getSt_nm() + "역";
            String url = BASE_URL + query;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + API_KEY);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[COORD FETCH] No response for station: {}", station.getSt_nm());
                return Optional.empty();
            }
            // JSON → 객체 변환 (Gson)
            KakaoApiResponse result = gson.fromJson(response.getBody(), KakaoApiResponse.class);

            return result.documents.stream()
                    .filter(doc -> "지하철역".equals(doc.category_group_name))
                    .findFirst();

        } catch (Exception e) {
            log.error("[COORD FETCH] Error fetching coords for {}: {}", station.getSt_nm(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     *  kakao 지하철역 id  추출
     */
    private Optional<String> extractOgUrlId(String placeUrl) {
        try {
            Document doc = Jsoup.connect(placeUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();

            Element og = doc.selectFirst("meta[property=og:url]");
            if (og != null) {
                String content = og.attr("content");
                return Optional.of(content.replace("https://place.map.kakao.com/", ""));
            }
        } catch (Exception e) {
            log.warn("[OG URL] Failed to parse og:url from {}: {}", placeUrl, e.getMessage());
        }
        return Optional.empty();
    }

}
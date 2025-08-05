package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kr.co.saramin.lab.commutingcrw.constant.Region;
import kr.co.saramin.lab.commutingcrw.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 지하철 원천 데이터 생성을 위한 메인 클래스.
 * 이 클래스는 데이터 처리 로직을 조율하며, 실제 작업은 별도의 서비스 클래스에서 수행됩니다.
 * 주요 기능:
 * - 지하철 환승 데이터 생성
 * - 좌표 데이터 생성
 * - 신규 역 데이터 생성
 * - 전체 지하철 JSON 파일 생성
 * - 통근 경로 데이터 생성
 * - 파일 검증
 *
 * 재사용성을 위해 데이터 로딩/쓰기, API 클라이언트, 데이터 프로세싱을 별도 클래스로 분리했습니다.
 * 가독성을 위해 메서드를 세밀하게 나누고, 주석을 상세히 추가했습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MakeRawData {

    private static final String SUBWAY_PRE_PATH = "/Users/user/commuting/2025/final";
    private static final String ALL_SUBWAY = "subway.json";
    private static final String BUSAN_SUBWAY = "subway_busan.json";
    private static final String DAEJEON_SUBWAY = "subway_daejeon.json";
    private static final String DAEGU_SUBWAY = "subway_daegu.json";
    private static final String GWANGJU_SUBWAY = "subway_gwangju.json";
    private static final String METRO_DATA = "metro_sri_coord.csv";
    private static final String ETC_DATA = "etc_sri_coord.csv";

    private final DataIoService dataIoService;
    private final SubwayDataProcessor subwayDataProcessor;
    private final KakaoApiClient kakaoApiClient;
    private final CommutingRouteService commutingRouteService;
    private final Gson gson = new Gson();

    static {
        // SSL 인증서 검증을 우회하기 위한 초기화 (개발 환경에서만 사용 권장, 프로덕션에서는 보안 강화 필요)
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
            log.error("SSL 초기화 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 모든 지하철 원천 데이터 생성 프로세스를 실행합니다.
     * 각 단계는 주석 처리되어 있으므로 필요에 따라 활성화/비활성화 가능.
     */
    public void all() {
        // 지하철 환승역 데이터 생성 (필수 파일: metro_sri.csv)
//         subwayDataProcessor.makeTransferData();

        // 좌표 데이터
        // 통근경로/시간을 구할때 필요한 id 값 확인 하여 전체 데이터 저장.
//         subwayDataProcessor.makeCoordinateData();  //  kakao api 활용

        // 최종 파일 검증 (빈 데이터 체크)
//        fileCheck(METRO_DATA);

        // 신규 역 데이터 생성 (콘솔 출력으로 복사하여 사용)
//         subwayDataProcessor.makeNewStation();      //  신규역 데이터 수집

        // 전체 지하철 JSON 파일 생성
        //  csv ->  json (최종파일로 es 색인용)
//         makeSubwayAll(METRO_DATA,ALL_SUBWAY);
//        makeSubwayAll(ETC_DATA,DAEGU_SUBWAY, "5");
//        makeSubwayAll(ETC_DATA,BUSAN_SUBWAY, "4");
//        makeSubwayAll(ETC_DATA,DAEJEON_SUBWAY, "8");
//        makeSubwayAll(ETC_DATA,GWANGJU_SUBWAY, "6");

        // 통근 경로 데이터 생성
//        makeCommuting();

        //  통근파일 체크
//        checkCommutingFile();

        //  output file 검증
//        dataIoService.validateCommutingData();
    }

    /**
     *  통근데이터 파일 체크.
     */
    @SneakyThrows
    public void checkCommutingFile() {
        String[] regions = {"busan","daejeon","daegu","gwangju"};
        for (String region : regions) {
            List<SubwayVo> stationList = dataIoService.loadSubwayMeta("subway_etc.json")
                    .stream().filter(subwayVo -> region.equals(subwayVo.getRegion()))
                    .collect(Collectors.toList());
            String filepath = "/Users/user/commuting/2025/final" + "/routes_" + region;
            int folderFileCnt = dataIoService.countJsonFiles(filepath);
            log.info("region : {}, 총 지하철역 건수 : {}, 통근데이터 파일건수 : {}", region, stationList.size(), folderFileCnt);
            //  폴더별 각 파일의 건수체크
            dataIoService.validateCommutingJsonPath(filepath);
        }

    }

    /**
     * 전체 지하철 역 메타데이터를 JSON 파일로 생성합니다.
     * 입력: metro_sri_coord.csv
     * 출력: subway.json
     */
    @SneakyThrows
    public void makeSubwayAll(String fileName, String outputFileName, String region) {
        // 좌표 데이터 로드
        List<Subway> records = dataIoService.readCsv(fileName, parts -> Subway.builder()
                .node_id(parts[0])
                .st_id(parts[1])
                .node_nm(parts[2])
                .st_nm(parts[3])
                .coords(new Coords(parts[5], parts[4]))
                .external_id(parts[6])
                .build()).stream().filter(m -> m.getNode_id().startsWith(region)).collect(Collectors.toList());

        // st_nm 기준으로 그룹핑하여 SubwayVo 생성
        Map<String, List<Subway>> grouped = records.stream()
                .collect(Collectors.groupingBy(Subway::getSt_nm));

        List<SubwayVo> stationMeta = grouped.entrySet().stream()
                .map(entry -> SubwayVo.builder()
                        .st_nm(entry.getKey())
                        .is_transfer(entry.getValue().size() > 1)
                        .info(entry.getValue())
                        .region(Region.fromNodeId(entry.getValue().get(0).getNode_id()).name())
                        .reg_dt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                        .up_dt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                        .build())
                .collect(Collectors.toList());

        // JSON으로 변환 및 쓰기
        dataIoService.writeJson(outputFileName, stationMeta);

        log.info("JSON 파일 생성 완료: {}", outputFileName);
    }

    /**
     * 통근 경로 데이터를 생성합니다.
     * 각 출발역에서 모든 도착역으로의 경로를 계산하고 JSON으로 저장.
     * 입력: subway.json 또는 subway_test.json
     * 출력: routes/{st_id}_routes.json
     */
    @SneakyThrows
    public void makeCommuting(String subwayFile, Region region) {
        // 지하철 메타데이터 로드 (테스트 파일 사용 가능)
        List<SubwayVo> stationList = dataIoService.loadSubwayMeta(subwayFile);

        // 모든 역 정보를 맵으로 변환 (external_id 기준)
        Map<String, Subway> stationMap = stationList.stream()
                .flatMap(vo -> vo.getInfo().stream())
                .collect(Collectors.toMap(Subway::getExternal_id, s -> s, (a, b) -> a));

        // 에러 로그 파일 준비
        Path errorLogPath = Paths.get("./logs/failures.log");
        Files.createDirectories(errorLogPath.getParent());
        try (BufferedWriter errorLog = Files.newBufferedWriter(errorLogPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            for (SubwayVo fromVo : stationList) {
                List<CommutingAllData> results = new ArrayList<>();
                Subway from = fromVo.getInfo().get(0);
                for (SubwayVo toVo : stationList) {
                    Subway to = toVo.getInfo().get(0);
                    if (!from.getExternal_id().equals(to.getExternal_id())) {
                        try {
                            CommutingAllData result = commutingRouteService.processRoute(from, to, stationMap, region.name());
                            if (result != null) {
                                results.add(result);
                            } else {
                                errorLog.write(String.join(",", "[PROCESS FAIL]", from.getExternal_id(), to.getExternal_id()) + "\n");
                            }
                        } catch (Exception e) {
                            errorLog.write(String.join(",", "[EXCEPTION]", from.getExternal_id(), to.getExternal_id(), e.getMessage()) + "\n");
                            log.error("통근 경로 처리 중 오류: from={}, to={}, error={}", from.getSt_nm(), to.getSt_nm(), e.getMessage(), e);
                        }
                    }
                    Thread.sleep(200);
                }

                String commutingPrePath = SUBWAY_PRE_PATH + "/routes_" + region.name();
                // 결과 JSON 쓰기
                Path outputPath = Paths.get(commutingPrePath, from.getSt_id() + "_routes.json");
                dataIoService.writeJson(outputPath.toString(), results);
            }
        }
    }

    /**
     *  기분석된 결과가 있으면 pass
     */
    public boolean checkCommutingFile(Subway from) {
        try {
            // 파일 경로 생성
            Path filePath = Paths.get("/Users/wandol/commuting/routes_daegu", from.getSt_id() + "_routes.json");
            List<CommutingAllData> fileRoutes = dataIoService.fromCommutingData(filePath);
            if(fileRoutes == null)  return true;
            // 배열 크기가 649인지 확인
            int arraySize = fileRoutes.size();
            if (arraySize == 649) {
                log.info("파일 {}의 배열 크기가 649입니다.", filePath);
                return false;
            } else {
                log.warn("파일 {}의 배열 크기가 649가 아님: {}", filePath, arraySize);
                return true;
            }

        } catch (Exception e) {
            // 파일이 없거나 읽기 실패 시
            log.error("파일 확인 중 오류: from={}, error={}", from.getSt_nm(), e.getMessage(), e);
            return true;
        }
    }

    /**
     * 최종 지하철 원천 파일의 빈 데이터를 검증합니다.
     * 빈 필드가 있는 레코드를 로그로 출력.
     */
    public void fileCheck(String fileName) {
        List<MetroSriVO> stationList = dataIoService.readCsv(fileName,
                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]));

        for (MetroSriVO m : stationList) {
            if (m.getNode_nm().isEmpty() && m.getExternal_id().isEmpty()
                    && m.getNode_id().isEmpty() && m.getSt_nm().isEmpty()
                    && m.getSt_id().isEmpty() && m.getSt_lon().isEmpty()
                    && m.getSt_lat().isEmpty()) {
                log.info("빈 데이터 발견: {}", m);
            }
        }
    }

    @SneakyThrows
    public void newSubwayAppend(Region region, String jsonFile, MetroSriVO subwayData) {
        final Path path = Paths.get(jsonFile);
        final String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 1. 기존 JSON 데이터 로딩
        List<SubwayVo> existingList = loadExistingSubwayList(path);

        // 2. Kakao API로 좌표 조회
        KakaoApiResponse.Document doc = kakaoApiClient.fetchSubwayCoordinates(subwayData)
                .orElseThrow(() -> new IllegalStateException("좌표 조회 실패: " + subwayData.getSt_nm()));
        String ogId = kakaoApiClient.extractOgUrlId(doc.place_url).orElse("");

        // 3. Subway 객체 생성
        Subway newSubway = createSubway(subwayData, doc, ogId);

        // 4. 기존 환승역 여부 검사 및 추가
        boolean isMerged = existingList.stream()
                .filter(vo -> vo.getSt_nm().equals(subwayData.getSt_nm()))
                .findFirst()
                .map(vo -> vo.getInfo().add(newSubway))
                .orElse(false);

        // 5. 신규 역이면 SubwayVo 생성 후 추가
        if (!isMerged) {
            SubwayVo newVo = new SubwayVo();
            newVo.setSt_nm(subwayData.getSt_nm());
            newVo.set_transfer(false); // 최초 추가는 단일 info이므로 false
            newVo.setInfo(new ArrayList<>(List.of(newSubway)));
            newVo.setRegion(region.name());
            newVo.setReg_dt(now);
            newVo.setUp_dt(now);
            existingList.add(newVo);
        }

        // 6. JSON 저장
        dataIoService.writeJson(jsonFile, existingList);
        log.info("Subway 데이터 추가 완료: st_nm={}, node_nm={}", subwayData.getSt_nm(), subwayData.getNode_nm());
    }

    private List<SubwayVo> loadExistingSubwayList(Path path) {
        if (!Files.exists(path)) return new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<SubwayVo>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (Exception e) {
            log.warn("기존 JSON 파싱 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Subway createSubway(MetroSriVO data, KakaoApiResponse.Document doc, String ogId) {
        Subway subway = new Subway();
        subway.setNode_id(data.getNode_id());
        subway.setSt_id(data.getSt_id());
        subway.setNode_nm(data.getNode_nm());
        subway.setSt_nm(data.getSt_nm());
        subway.setCoords(new Coords(doc.x, doc.y));
        subway.setExternal_id(ogId);
        return subway;
    }


}
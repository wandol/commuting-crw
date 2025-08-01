package kr.co.saramin.lab.commutingcrw.module;

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

    private static final String ALL_SUBWAY = "subway.json";
    private static final String ETC_SUBWAY = "subway_etc.json";
    private static final String METRO_DATA = "metro_sri_coord.csv";
    private static final String ETC_DATA = "etc_sri_coord.csv";

    private final DataIoService dataIoService;
    private final SubwayDataProcessor subwayDataProcessor;
    private final CommutingRouteService commutingRouteService;

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
//        makeSubwayAll(ETC_DATA,ETC_SUBWAY);

        // 통근 경로 데이터 생성
//        makeCommuting();



        //  output file 검증
//        dataIoService.validateCommutingData();
    }

    /**
     * 전체 지하철 역 메타데이터를 JSON 파일로 생성합니다.
     * 입력: metro_sri_coord.csv
     * 출력: subway.json
     */
    @SneakyThrows
    public void makeSubwayAll(String fileName, String outputFileName) {
        // 좌표 데이터 로드
        List<Subway> records = dataIoService.readCsv(fileName, parts -> Subway.builder()
                .node_id(parts[0])
                .st_id(parts[1])
                .node_nm(parts[2])
                .st_nm(parts[3])
                .coords(new Coords(parts[5], parts[4]))
                .external_id(parts[6])
                .build());

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
    private void makeCommuting() {
        // 지하철 메타데이터 로드 (테스트 파일 사용 가능)
        List<SubwayVo> stationList = dataIoService.loadSubwayMeta("subway_test.json"); // 또는 ALL_SUBWAY 사용

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
                            CommutingAllData result = commutingRouteService.processRoute(from, to, stationMap, "metro");
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
                }

                // 결과 JSON 쓰기
                Path outputPath = Paths.get("routes", from.getSt_id() + "_routes.json");
                dataIoService.writeJson(outputPath.toString(), results);
            }
        }
    }

    /**
     * 최종 지하철 원천 파일의 빈 데이터를 검증합니다.
     * 빈 필드가 있는 레코드를 로그로 출력.
     */
    private void fileCheck(String fileName) {
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
}
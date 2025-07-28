package kr.co.saramin.lab.commutingcrw.module;

import kr.co.saramin.lab.commutingcrw.vo.KakaoApiResponse;
import kr.co.saramin.lab.commutingcrw.vo.MetroSriVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 지하철 데이터 프로세싱을 담당하는 클래스.
 * 환승 데이터 생성, 좌표 데이터 생성, 신규 역 생성 등을 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class SubwayDataProcessor {

    private static final String METRO_SRI = "metro_sri.csv";
    private static final String SUBWAY_TRANS = "subway_trans.csv";
    private static final String METRO_DATA = "metro_sri_coord.csv";

    private final DataIoService dataIoService;
    private final KakaoApiClient kakaoApiClient;

    /**
     * 환승역 데이터를 생성합니다.
     * 입력: metro_sri.csv
     * 출력: subway_trans.csv (st_nm|st_id,st_id,...)
     */
    public void makeTransferData() {
        List<MetroSriVO> sriData = dataIoService.readCsv(METRO_SRI,
                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));

        Map<String, List<String>> transferData = sriData.stream()
                .collect(Collectors.groupingBy(MetroSriVO::getSt_nm,
                        Collectors.mapping(MetroSriVO::getSt_id, Collectors.toList())))
                .entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<String> lines = transferData.entrySet().stream()
                .map(entry -> entry.getKey() + "|" + String.join(",", entry.getValue()))
                .collect(Collectors.toList());

        dataIoService.writeCsv(SUBWAY_TRANS, lines);
    }

    /**
     * 각 역의 좌표와 external_id를 추가하여 새로운 CSV 생성.
     * 입력: metro_sri.csv
     * 출력: metro_sri_coord.csv
     */
    @SneakyThrows
    public void makeCoordinateData() {
        List<MetroSriVO> stationList = dataIoService.readCsv(METRO_SRI,
                parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));

        List<String> outputLines = new ArrayList<>();
        for (MetroSriVO station : stationList) {
            Optional<KakaoApiResponse.Document> docOpt = kakaoApiClient.fetchSubwayCoordinates(station);

            if (docOpt.isEmpty()) {
                outputLines.add(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), "", "", ""));
            } else {
                KakaoApiResponse.Document doc = docOpt.get();
                String ogId = kakaoApiClient.extractOgUrlId(doc.place_url).orElse("");
                outputLines.add(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), doc.x, doc.y, ogId));
                log.info("좌표 조회 성공: station={}, x={}, y={}", station.getSt_nm(), doc.x, doc.y);
            }
            Thread.sleep(200); // API rate limit 방지
        }

        dataIoService.writeCsv(METRO_DATA, outputLines);
    }

    /**
     * 신규 역 데이터를 생성하고 콘솔에 출력합니다.
     * 출력 형식: node_id|st_id|node_nm|st_nm|x|y|external_id
     * Kakao API로 좌표 조회.
     */
    public void makeNewStation() {
        MetroSriVO station = new MetroSriVO();
        station.setSt_id("지하철역id"); // 실제 ID로 변경
        station.setNode_id("호선id"); // 실제 ID로 변경
        station.setNode_nm("1호선"); // 실제 노선명으로 변경
        station.setSt_nm("신규역"); // 실제 역명으로 변경

        Optional<KakaoApiResponse.Document> docOpt = kakaoApiClient.fetchSubwayCoordinates(station);

        if (docOpt.isEmpty()) {
            System.out.println(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), "", "", ""));
        } else {
            KakaoApiResponse.Document doc = docOpt.get();
            String ogId = kakaoApiClient.extractOgUrlId(doc.place_url).orElse("");
            System.out.println(String.join("|", station.getNode_id(), station.getSt_id(), station.getNode_nm(), station.getSt_nm(), doc.x, doc.y, ogId));
        }
    }
}
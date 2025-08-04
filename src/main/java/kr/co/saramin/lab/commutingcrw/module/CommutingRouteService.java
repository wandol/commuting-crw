package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kr.co.saramin.lab.commutingcrw.vo.CommutingAllData;
import kr.co.saramin.lab.commutingcrw.vo.Subway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 통근 경로 계산을 담당하는 서비스 클래스.
 * Kakao 지하철 경로 API 호출 및 파싱.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CommutingRouteService {

    private final KakaoApiClient kakaoApiClient;
    private final Gson gson = new Gson();

    /**
     * 출발지에서 도착지까지의 통근 경로를 계산합니다.
     * Kakao API 호출 (캐싱 지원: 기존 파일 사용)
     * @param from 출발지 Subway
     * @param to 도착지 Subway
     * @param stationMap external_id 기반 역 맵
     * @param region 지역 (e.g., "metro")
     * @return CommutingAllData 객체, 실패 시 null
     */
    public CommutingAllData processRoute(Subway from, Subway to, Map<String, Subway> stationMap, String region) {
        try {
            String apiUrl = "https://map.kakao.com/api/subway/routes/current/daegu/"
                    + from.getExternal_id() + "--08:00:00/" + to.getExternal_id()
                    + "?resultTimeFormat=TIME_STRING&findingType=NORMAL&dayType=WEEKDAY";

            // 응답 캐싱 디렉토리 및 파일
            Path debugDir = Paths.get("route_api_responses", from.getSt_id());
            Files.createDirectories(debugDir);
            String fileName = from.getExternal_id() + "_TO_" + to.getExternal_id() + ".json";
            Path responseFile = debugDir.resolve(fileName);

            String json;
            if (Files.exists(responseFile)) {
                json = Files.readString(responseFile, StandardCharsets.UTF_8);
                log.debug("캐싱된 응답 사용: from={}, to={}", from.getSt_nm(), to.getSt_nm());
            } else {
                json = kakaoApiClient.fetch(apiUrl);
                Files.write(responseFile, json.getBytes(StandardCharsets.UTF_8));
                log.debug("API 호출 성공: from={}, to={}", from.getSt_nm(), to.getSt_nm());
            }

            // JSON 파싱
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> tree = gson.fromJson(json, type);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) tree.get("body");
            @SuppressWarnings("unchecked")
            Map<String, Object> routes = (Map<String, Object>) body.get("routes");
            @SuppressWarnings("unchecked")
            Map<String, Object> shortest = (Map<String, Object>) routes.get("shortest_time");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sections = (List<Map<String, Object>>) shortest.get("sections");

            // 경로, 환승 정보 추출
            List<Subway> path = new ArrayList<>();
            List<String> transferLines = new ArrayList<>();
            List<String> transferStNms = new ArrayList<>();
            List<String> transferStCds = new ArrayList<>();

            for (int i = 0; i < sections.size(); i++) {
                Map<String, Object> section = sections.get(i);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) section.get("nodes");
                String lineName = (String) section.get("line_name");
                if (i > 0) transferLines.add("subway_transfer");
                transferLines.add(lineName);

                for (Map<String, Object> node : nodes) {
                    String sid = (String) node.get("station_id");
                    Subway st = stationMap.get(sid);
                    if (st != null) path.add(st);
                }

                if (i < sections.size() - 1) {
                    Map<String, Object> nextSection = sections.get(i + 1);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> nextNodes = (List<Map<String, Object>>) nextSection.get("nodes");
                    if (!nextNodes.isEmpty()) {
                        String transferExtId = (String) nextNodes.get(0).get("station_id");
                        Subway transferStation = stationMap.get(transferExtId);
                        if (transferStation != null) {
                            transferStNms.add(transferStation.getSt_nm());
                            transferStCds.add(transferStation.getSt_id());
                        }
                    }
                }
            }
            Set<String> fromStIdSet = stationMap.values().stream()
                    .filter(s -> s.getSt_nm().equals(from.getSt_nm()))
                    .map(Subway::getSt_id)
                    .collect(Collectors.toSet());
            Set<String> toStIdSet = stationMap.values().stream()
                    .filter(s -> s.getSt_nm().equals(to.getSt_nm()))
                    .map(Subway::getSt_id)
                    .collect(Collectors.toSet());

            LinkedHashSet<String> pathStIds = new LinkedHashSet<>();
            for (Subway p : path) {
                pathStIds.add(p.getSt_id());
            }

            // reg_dt/up_dt: 현재 시간 형식 (예: ISO_LOCAL_DATE_TIME)
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            return CommutingAllData.builder()
                    .from_st_id(fromStIdSet)
                    .to_st_id(toStIdSet)
                    .totalCost((int) Math.ceil(((Double) shortest.get("time_required")) / 60.0))
                    .path(path)
                    .path_st_ids(pathStIds)
                    .transferNode(String.join(",", transferLines))
                    .transferStNm(String.join(",", transferStNms))
                    .transferStId(String.join(",", transferStCds))
                    .region(region)
                    .reg_dt(now)
                    .up_dt(now)
                    .build();

        } catch (Exception e) {
            log.error("경로 계산 실패: from={}, to={}, error={}", from.getSt_nm(), to.getSt_nm(), e.getMessage(), e);
            return null;
        }
    }
}
package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import kr.co.saramin.lab.commutingcrw.vo.KakaoApiResponse;
import kr.co.saramin.lab.commutingcrw.vo.MetroSriVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Kakao API 호출을 담당하는 클라이언트 클래스.
 * 좌표 조회, place_url 파싱 등을 재사용 가능하게 분리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class KakaoApiClient {

    private static final String API_KEY = "64a3f2a2e19a6652a0645e5f82215213";
    private static final String BASE_URL = "https://dapi.kakao.com/v2/local/search/keyword?query=";
    private final RestTemplate restTemplate;
    private final Gson gson = new Gson();

    /**
     * 지하철 역의 좌표와 카테고리 정보를 Kakao API로 조회합니다.
     * @param station MetroSriVO 객체 (node_nm, st_nm 사용)
     * @return Document 객체 (좌표 등 포함), 실패 시 empty
     */
    public Optional<KakaoApiResponse.Document> fetchSubwayCoordinates(MetroSriVO station) {
        try {
            String query = station.getNode_nm() + " " + station.getSt_nm() + "역";
            String url = BASE_URL + query; // 쿼리 인코딩 추가
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + API_KEY);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Kakao API 응답 없음: station={}", station.getSt_nm());
                return Optional.empty();
            }

            KakaoApiResponse result = gson.fromJson(response.getBody(), KakaoApiResponse.class);
            return result.documents.stream()
                    .filter(doc -> "지하철역".equals(doc.category_group_name))
                    .findFirst();

        } catch (Exception e) {
            log.error("Kakao API 호출 실패: station={}, error={}", station.getSt_nm(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Kakao place_url에서 og:url ID를 추출합니다.
     * @param placeUrl place_url 문자열
     * @return ID 문자열, 실패 시 empty
     */
    public Optional<String> extractOgUrlId(String placeUrl) {
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
            log.warn("og:url 파싱 실패: url={}, error={}", placeUrl, e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * 일반 HTTP GET 요청으로 데이터를 fetch합니다.
     * @param urlStr 요청 URL
     * @return 응답 문자열
     * @throws IOException 요청 실패 시
     */
    public String fetch(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (InputStream in = conn.getInputStream();
             InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
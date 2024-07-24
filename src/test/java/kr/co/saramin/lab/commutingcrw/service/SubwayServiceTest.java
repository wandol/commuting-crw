package kr.co.saramin.lab.commutingcrw.service;

import com.google.gson.Gson;
import kr.co.saramin.lab.commutingcrw.vo.MetroDataVO;
import kr.co.saramin.lab.commutingcrw.vo.SeoulMetroVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


class SubwayServiceTest {

    @Test
    public void test() {
        RestTemplate restTemplate = new RestTemplate();
        String baseUrl = "http://www.seoulmetro.co.kr/kr/getRouteSearchResult.do";
        // UriComponentsBuilder를 사용하여 URL과 파라미터를 설정
        UriComponentsBuilder url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("departureId", "0321")
                .queryParam("arrivalId", "0425")
                .queryParam("sKind", "1");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Accept", MediaType.ALL_VALUE);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url.toUriString(), HttpMethod.GET, entity, String.class);
        System.out.println(response.getBody());
    }

    @Test
    public void test1() throws URISyntaxException, IOException {

        String metroInFile = "C:\\develop\\commuting\\metro_data.csv";
        //  내부 데이터
        List<MetroDataVO> metroDataVOList = Files.readAllLines(Paths.get(metroInFile)).stream()
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


        URL url = null;
        try {
            url = new URL("https://data.seoul.go.kr/dataList/dataView.do?onepagerow=1000&srvType=S&infId=OA-121&serviceKind=1&pageNo=1&gridTotalCnt=785&ssUserId=SAMPLE_VIEW&strWhere=&strOrderby=&filterCol=%ED%95%84%ED%84%B0%EC%84%A0%ED%83%9D&txtFilter=");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url.toURI(), String.class);
        Gson gson = new Gson();
        SeoulMetroVO response = gson.fromJson(responseEntity.getBody(), SeoulMetroVO.class);
        checkSt(metroDataVOList, response.getList());

        List<MetroDataVO> m = mappingCode(metroDataVOList, response.getList());
    }

    private void checkSt(List<MetroDataVO> metroDataVOList, List<SeoulMetroVO.Station> list) {
        Map<String, List<SeoulMetroVO.Station>> map = list.stream()
                .filter(Objects::nonNull)
                .filter(station -> station.getStationNm() != null)
                .collect(Collectors.groupingBy(SeoulMetroVO.Station::getStationNm));
        Map<String, List<MetroDataVO>> map2 = metroDataVOList.stream()
                .collect(Collectors.groupingBy(MetroDataVO::getSt_nm));
        for (SeoulMetroVO.Station station : list) {
            if(station == null){

            }
            if(map2.get(station.getStationNm()) == null){
                System.out.println(station);
            }
        }
        for (MetroDataVO metroDataVO : metroDataVOList) {
            String key = checkStNm(metroDataVO.getSt_nm());
            if(map.get(key) == null){
                System.out.println(key);
            }
        }
    }


    private List<MetroDataVO> mappingCode(List<MetroDataVO> metroDataVOList, List<SeoulMetroVO.Station> seoulMetroVOList) {
        if(Objects.nonNull(metroDataVOList) && Objects.nonNull(seoulMetroVOList)){

            Map<String, List<SeoulMetroVO.Station>> map = seoulMetroVOList.stream()
                    .filter(Objects::nonNull)
                    .filter(station -> station.getStationNm() != null)
                    .collect(Collectors.groupingBy(SeoulMetroVO.Station::getStationNm));

            for (MetroDataVO metroDataVO : metroDataVOList) {
                String key = checkStNm(metroDataVO.getSt_nm());

                if(map.get(key) != null){
                    metroDataVO.setSt_id(map.get(key).get(0).getStationCd());
                }else{
                    metroDataVO.setSt_id("reject");
                    System.out.println("?? " + metroDataVO);
                }
            }
        }
        metroDataVOList.stream().forEach( metroDataVO -> {
            System.out.println(metroDataVO.toString());
        });
        return metroDataVOList;
    }

    /**
     * 이름 매핑
     * 지하철역 변경 또는 이름이 다른 역 매핑
     * @param stNm
     * @return
     */
    private String checkStNm(String stNm) {
        if(stNm.equals("총신대입구(이수)")) return "총신대입구";
        if(stNm.equals("전대∙에버랜드")) return "전대.에버랜드";
        if(stNm.equals("시청∙용인대")) return "시청.용인대";
        if(stNm.equals("운동장∙송담대")) return "용인중앙시장";
        if(stNm.equals("쌍용(나사렛대)")) return "쌍용";
        if(stNm.equals("경기도청 북부청사")) return "경기도청북부청사";
        if(stNm.equals("4∙19민주묘지")) return "4·19민주묘지";
        return stNm;
    }

}
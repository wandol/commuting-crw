package kr.co.saramin.lab.commutingcrw.service;

import kr.co.saramin.lab.commutingcrw.constant.Region;
import kr.co.saramin.lab.commutingcrw.module.MakeRawData;
import kr.co.saramin.lab.commutingcrw.vo.MetroSriVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MetroService {

    private final MakeRawData makeRawData;

    private static final String METRO_SUBWAY = "202508/subway.json";
    private static final String BUSAN_SUBWAY = "202508/subway_busan.json";
    private static final String DAEJEON_SUBWAY = "202508/subway_daejeon.json";
    private static final String DAEGU_SUBWAY = "202508/subway_daegu.json";
    private static final String GWANGJU_SUBWAY = "202508/subway_gwangju.json";

    private static final String METRO_DATA = "metro_sri_coord.csv";
    private static final String ETC_DATA = "etc_sri_coord.csv";

    public void makeRawData() {

        // 신규역이 추가 되었을때
        // --> 해당 지하철역 내부데이터를 알아야 함. 예시파일 metro_sri_coord.csv
        //  103|10334|서울 3호선|매봉  node_id|st_id|node_nm|st_nm
        //  해당 데이터로 좌표와 카카오 지하철 id를 찾아 타겟 지역 지하철역 데이터에 저장.
        makeRawData.newSubwayAppend(Region.busan, BUSAN_SUBWAY, new MetroSriVO("111", "213123", "부산 2호선", "금곡"));
        //  통근데이터 생성
        //  지역별로 args를 달리하여 전달.
        //  metro
//        makeRawData.makeCommuting(BUSAN_SUBWAY, Region.busan);
    }

}

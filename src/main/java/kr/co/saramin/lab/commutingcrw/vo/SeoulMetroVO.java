package kr.co.saramin.lab.commutingcrw.vo;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * User: wandol<br/>
 * Date: 2022/12/16<br/>
 * Time: 1:02 PM<br/>
 * Desc:
 */
@Data
@Builder
public class SeoulMetroVO {
    // JSON 데이터를 담기 위한 클래스 정의
    String result;
    Page page;
    List<Station> list;

    @Data
    public class Page {
        int pageNo;
        int pageCount;
        int totalCount;
        int listCount;
    }

    @Data
    public
    class Station {
        @SerializedName("LINE_NUM")
        String lineNum;

        @SerializedName("RONUM")
        String roNum;

        @SerializedName("STATION_CD")
        String stationCd;

        @SerializedName("STATION_NM")
        String stationNm;

        @SerializedName("FR_CODE")
        String frCode;
    }
}

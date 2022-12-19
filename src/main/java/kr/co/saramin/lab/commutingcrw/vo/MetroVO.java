package kr.co.saramin.lab.commutingcrw.vo;

import lombok.Builder;
import lombok.Data;

/**
 * User: wandol<br/>
 * Date: 2022/12/16<br/>
 * Time: 1:02 PM<br/>
 * Desc:
 */
@Data
@Builder
public class MetroVO {
    private String code;
    private String sri_code;
    private String subNm;
    private String line;
    private String metro_code;
    private String otherCd;
}

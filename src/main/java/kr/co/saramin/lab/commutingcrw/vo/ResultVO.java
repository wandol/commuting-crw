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
public class ResultVO {
    private String startNodeNmSearch;
    private String startStNmSearch;
    private String endCodeSearch;
    private String startStNm;
    private String endStNm;
    private String pathsNm;
    private String pathsCd;
    private String totalCost;
    private String transferNode;
    private String transferStNm;
    private String transferStCd;

}

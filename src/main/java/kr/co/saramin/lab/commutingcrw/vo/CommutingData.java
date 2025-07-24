package kr.co.saramin.lab.commutingcrw.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommutingData {
    private String startNodeNmSearch;
    private String startStNmSearch;
    private String endCodeSearch;
    private String startStNm;
    private String endStNm;
    private List<String> pathsNm;
    private List<String> pathsCd;
    private int totalCost;
    private List<String> transferNode;
    private List<String> transferStNm;
    private List<String> transferStCd;
}
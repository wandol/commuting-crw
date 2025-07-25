package kr.co.saramin.lab.commutingcrw.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommutingAllData {
    private String fromSt;
    private String fromId;
    private String fromNodeNm;
    private String fromNodeId;
    private String to;
    private String toId;
    private String toNodeNm;
    private String toNodeId;
    private int totalCost;
    private String x_coordinate;
    private String y_coordinate;
    private List<String> pathsNm;
    private List<String> pathsCd;
    private String transferNode;
    private String transferStNm;
    private String transferStCd;
    private List<Map<String, String>> external_route;
}
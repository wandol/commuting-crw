package kr.co.saramin.lab.commutingcrw.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommutingAllData {
    Set<String> from_st_id;
    Set<String> to_st_id;
    private int totalCost;
    List<Subway> path;
    LinkedHashSet<String> path_st_ids;
    private String transferNode;
    private String transferStNm;
    private String transferStId;
    private String region;
    private String reg_dt;
    private String up_dt;
}
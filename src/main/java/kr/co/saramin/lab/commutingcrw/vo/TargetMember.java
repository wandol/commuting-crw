package kr.co.saramin.lab.commutingcrw.vo;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetMember {
    private String mem_idx;
    private String csn;
    private List<String> up1;
    private List<String> up1_nm;
    private List<String> up2;
    private List<String> up2_nm;
    private List<String> up3;
    private List<String> up3_nm;
    private String st_nm;
    private String takeMin;
}

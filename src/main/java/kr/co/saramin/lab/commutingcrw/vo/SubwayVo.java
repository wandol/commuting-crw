package kr.co.saramin.lab.commutingcrw.vo;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubwayVo {
    String st_nm;
    boolean is_transfer;
    List<Subway> info;
    private String region;
    private String reg_dt;
    private String up_dt;
}

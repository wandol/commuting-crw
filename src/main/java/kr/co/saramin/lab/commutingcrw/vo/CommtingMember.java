package kr.co.saramin.lab.commutingcrw.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommtingMember {
    private String mem_idx;
    private String csn;
    private int commuting_com;
    private int commuting_sindorim;
}

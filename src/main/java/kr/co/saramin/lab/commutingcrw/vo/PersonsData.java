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
public class PersonsData {
    private String mem_idx;
    private String x_coordinate;
    private String y_coordinate;
}

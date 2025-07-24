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
public class CompanyByMemberData {
    private String csn;
    private List<PersonsData> personsData;
    private String x_coordinate;
    private String y_coordinate;
}

package kr.co.saramin.lab.commutingcrw.vo;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetroSriVO {
    private String node_id;
    private String st_id;
    private String node_nm;
    private String st_nm;
    private String st_lat;
    private String st_lon;
    private String external_id;

    public MetroSriVO(String part, String part1, String part2, String part3) {
        this.node_id = part;
        this.st_id = part1;
        this.node_nm = part2;
        this.st_nm = part3;
    }
}

package kr.co.saramin.lab.commutingcrw.vo;

import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subway {
    private String node_id;
    private String st_id;
    private String node_nm;
    private String st_nm;
    private Coords coords;
    private String external_id;
}

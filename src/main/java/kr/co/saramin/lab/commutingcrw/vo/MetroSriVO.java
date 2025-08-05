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

    public MetroSriVO(String node_id, String st_id, String node_nm, String st_nm) {
        this.node_id = node_id;
        this.st_id = st_id;
        this.node_nm = node_nm;
        this.st_nm = st_nm;
    }
}

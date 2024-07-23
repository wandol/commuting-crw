package kr.co.saramin.lab.commutingcrw.vo;

import lombok.*;

/**
 * User: wandol<br/>
 * Date: 2022/12/16<br/>
 * Time: 1:02 PM<br/>
 * Desc:
 */
@Getter
@Setter
@ToString
@Builder
public class MetroDataVO {
    private String node_id;
    private String node_nm;
    private String st_id;
    private String st_nm;
    private String gps_x;
    private String gps_y;
    private String gps_x_real;
    private String gps_y_real;
    private String trans_type;
    private String area_nm;
    private String sri_subway_cd;

    @Override
    public String toString() {
        return node_id + '|' + node_nm + '|' + st_id + '|'
                + st_nm + '|'
                + gps_x + '|'
                + gps_y + '|'
                + gps_x_real + '|'
                + gps_y_real + '|'
                + trans_type + '|'
                + area_nm + '|'
                + sri_subway_cd ;
    }
}
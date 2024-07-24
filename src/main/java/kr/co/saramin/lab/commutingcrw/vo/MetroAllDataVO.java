package kr.co.saramin.lab.commutingcrw.vo;

import lombok.Builder;
import lombok.Data;

/**
 * User: wandol<br/>
 * Date: 2022/12/16<br/>
 * Time: 1:02 PM<br/>
 * Desc:
 */
@Data
@Builder
public class MetroAllDataVO {
    private String node_id;
    private String node_nm;
    private String st_id;
    private String st_nm;
    private String sri_st_code;
}

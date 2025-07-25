package kr.co.saramin.lab.commutingcrw.vo;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
public class KakaoApiResponse {
    public List<Document> documents;

    public static class Document {
        public String x;
        public String y;
        public String category_group_name;
        public String place_url;
    }
}

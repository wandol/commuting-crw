package kr.co.saramin.lab.commutingcrw.module;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import kr.co.saramin.lab.commutingcrw.vo.CommutingAllData;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
public class DataIndexer {

    private final ElasticsearchClient esClient;
    private static final String INDEX_NAME = "commuting_data";

    @SneakyThrows
    public void indexFolder(List<CommutingAllData> list, String fileName){
        List<BulkOperation> operations = new ArrayList<>();
        for (CommutingAllData data : list) {
            String docId = String.join("_", data.getFrom_st_id())
                    + "_" + String.join("_", data.getTo_st_id());
            operations.add(BulkOperation.of(b -> b
                    .index(idx -> idx
                            .index(INDEX_NAME)
                            .id(docId)
                            .document(data)
                    )
            ));
        }

        BulkRequest bulkRequest = new BulkRequest.Builder()
                .operations(operations)
                .build();

        BulkResponse response = esClient.bulk(bulkRequest);
        if (response.errors()) {
            log.error("일부 실패: {}", fileName);
            log.error(response.toString());
        } else {
            log.info("업로드 완료: {}", fileName);
        }
    }
}

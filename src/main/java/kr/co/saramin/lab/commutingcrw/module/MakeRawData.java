package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class MakeRawData {

    private final String METRO_SRI = "metro_sri.csv";
    private final String SUBWAY_TRANS = "subway_trans.csv";
    private static final String RAW_DIR = "raw";

    /**
     *  지하철 원천 데이터 생성.
     */
    public void all() {
        //  지하철 환승역 데이터 생성.
        //  필수 파일 - metro_sri.csv  node_id|st_id|node_nm|st_nm
        makeTransData();
    }

    /**
     * sri 지하철 명으로 중복을 체크하여 환승역 데이터 생성
     * output - st_nm|st_id,st_id,...
     */
    private void makeTransData() {
        try {
            List<MetroSriVO> sriData = readCsvWithReflection(METRO_SRI,
                    parts -> new MetroSriVO(parts[0], parts[1], parts[2], parts[3]));

            Map<String, List<String>> transferData = sriData.stream()
                    .collect(Collectors.groupingBy(
                            MetroSriVO::getSt_nm,
                            Collectors.mapping(MetroSriVO::getSt_id, Collectors.toList())
                    ))
                    .entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1) // 환승역만 필터링
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            transferData.forEach((stNm, stIds) ->
                    log.info("[TRANSFER GENERATION] Transfer station: {} with IDs {}",
                            stNm, String.join(",", stIds)));

            List<String> lines = transferData.entrySet().stream()
                    .map(entry -> entry.getKey() + "|" + String.join(",", entry.getValue()))
                    .collect(Collectors.toList());

            writeCsv(SUBWAY_TRANS, lines);

        } catch (Exception e) {
            log.error("[TRANSFER GENERATION] Error in makeTransData", e);
        }
    }

    /**
     * CSV 파일 읽기 (resources 하위), 라인 → 객체 매핑 함수 전달
     */
    private <T> List<T> readCsvWithReflection(String fileName, Function<String[], T> mapper) {
        try {
            Path path = Paths.get(RAW_DIR, fileName);
            try (Stream<String> lines = Files.lines(path)) {
                return lines.map(line -> line.split("\\|"))
                        .map(mapper)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("Failed to load file {}: {}", fileName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 리스트를 CSV 파일로 쓰기 (루트 기준)
     */
    private void writeCsv(String fileName, List<String> lines) {
        try {
            Path outputPath = Paths.get(RAW_DIR, fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, lines);
            log.info("[TRANSFER GENERATION] Written {} lines to {}", lines.size(), outputPath);
        } catch (IOException e) {
            log.error("Failed to write file {}: {}", fileName, e.getMessage(), e);
        }
    }
}
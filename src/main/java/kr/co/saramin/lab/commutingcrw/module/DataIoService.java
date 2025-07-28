package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.co.saramin.lab.commutingcrw.vo.SubwayVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 데이터 입출력을 담당하는 서비스 클래스.
 * CSV/JSON 읽기/쓰기를 재사용 가능하게 분리.
 * 모든 파일 경로는 RAW_DIR을 기반으로 함.
 */
@Slf4j
@Component
class DataIoService {

    private static final String RAW_DIR = "raw";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * CSV 파일을 읽어 객체 리스트로 변환합니다.
     * @param fileName 파일 이름 (RAW_DIR 하위)
     * @param mapper 라인을 객체로 매핑하는 함수
     * @param <T> 객체 타입
     * @return 객체 리스트
     */
    public <T> List<T> readCsv(String fileName, Function<String[], T> mapper) {
        try {
            Path path = Paths.get(RAW_DIR, fileName);
            try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
                return lines.map(line -> line.split("\\|"))
                        .map(mapper)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("CSV 파일 읽기 실패: file={}, error={}", fileName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 리스트를 CSV 파일로 씁니다.
     * @param fileName 파일 이름 (RAW_DIR 하위)
     * @param lines 쓰일 라인 리스트
     */
    public void writeCsv(String fileName, List<String> lines) {
        try {
            Path outputPath = Paths.get(RAW_DIR, fileName);
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, lines, StandardCharsets.UTF_8);
            log.info("CSV 파일 쓰기 완료: file={}, lines={}", fileName, lines.size());
        } catch (IOException e) {
            log.error("CSV 파일 쓰기 실패: file={}, error={}", fileName, e.getMessage(), e);
        }
    }

    /**
     * 객체 리스트를 JSON 파일로 씁니다.
     * @param fileName 파일 이름 (RAW_DIR 하위 또는 절대 경로)
     * @param data JSON으로 변환할 데이터
     */
    public void writeJson(String fileName, Object data) {
        try {
            Path outPath = fileName.startsWith("/") ? Paths.get(fileName) : Paths.get(RAW_DIR, fileName);
            Files.createDirectories(outPath.getParent());
            String json = gson.toJson(data);
            Files.write(outPath, json.getBytes(StandardCharsets.UTF_8));
            log.info("JSON 파일 쓰기 완료: file={}", fileName);
        } catch (IOException e) {
            log.error("JSON 파일 쓰기 실패: file={}, error={}", fileName, e.getMessage(), e);
        }
    }

    /**
     * 지하철 메타데이터 JSON 파일을 로드합니다.
     * @param filename 파일 이름 (RAW_DIR 하위)
     * @return SubwayVo 리스트
     * @throws IOException 파일 읽기 실패 시
     */
    public List<SubwayVo> loadSubwayMeta(String filename) throws IOException {
        Path path = Paths.get(RAW_DIR, filename);
        try (Reader reader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
            SubwayVo[] list = gson.fromJson(reader, SubwayVo[].class);
            return Arrays.asList(list);
        }
    }
}
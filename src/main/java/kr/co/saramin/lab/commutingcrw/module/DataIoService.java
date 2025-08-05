package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import kr.co.saramin.lab.commutingcrw.vo.CommutingAllData;
import kr.co.saramin.lab.commutingcrw.vo.Subway;
import kr.co.saramin.lab.commutingcrw.vo.SubwayVo;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
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
@AllArgsConstructor
class DataIoService {

    private final DataIndexer esIndexer;

    private static final String RAW_DIR = "raw";
    private static final String ROUTES_DIR = "raw/routes";
    private static final int EXPECTED_ROUTES_PER_FILE = 649;
    private static final int EXPECTED_FILE_COUNT = 650;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Path MISSING_LOG_FILE = Paths.get("logs/missing_routes.log");

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

    /**
     * routes 폴더 내 파일 개수와 각 파일의 통근 데이터 건수를 검증 (stationMap 기준).
     * - 파일 누락: stationMap의 st_id에 해당 파일 없는 경우 "누락 파일: st_id" 로깅.
     * - to 누락: 각 파일 내 to_st_id Set가 stationMap의 다른 id를 649개 커버하지 않으면 "파일명: 누락 to_st_id" 로깅.
     */
    @SneakyThrows
    public void validateCommutingData() {
        // 지하철 메타데이터 로드 (테스트 파일 사용 가능)
        List<SubwayVo> stationList = loadSubwayMeta("subway.json"); // 또는 ALL_SUBWAY 사용

        // 모든 역 정보를 맵으로 변환 (external_id 기준)
        Map<String, Subway> stationMap = stationList.stream()
                .flatMap(vo -> vo.getInfo().stream())
                .collect(Collectors.toMap(Subway::getSt_id, s -> s, (a, b) -> a));

        // stationMap에서 모든 st_id Set 추출 (기준 id 목록)
        Set<String> allStIds = stationMap.values().stream()
                .map(Subway::getSt_id)
                .collect(Collectors.toSet());

        // routes 폴더 내 JSON 파일 목록
        Path routesDir = Paths.get(ROUTES_DIR);
        //noinspection resource
        List<Path> jsonFiles = Files.list(routesDir)
                .filter(path -> path.toString().endsWith("_routes.json"))
                .collect(Collectors.toList());

        // 파일명에서 from_st_id 추출 맵 (파일명: {st_id}_routes.json → st_id)
        Set<String> existingFileStIds = jsonFiles.stream()
                .map(file -> file.getFileName().toString().replace("_routes.json", ""))
                .collect(Collectors.toSet());

        // 1. 파일 누락 검증: allStIds vs. existingFileStIds 비교
        Set<String> missingFiles = new HashSet<>(allStIds);
        missingFiles.removeAll(existingFileStIds);
        if (!missingFiles.isEmpty()) {
            log.error("누락 파일 개수: {}개", missingFiles.size());
        } else {
            log.info("파일 개수 검증 성공: {}개", EXPECTED_FILE_COUNT);
        }

        // 누락 로그 파일 준비
        Files.createDirectories(MISSING_LOG_FILE.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(MISSING_LOG_FILE, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            // 누락 파일 로깅
            for (String missingId : missingFiles) {
                writer.write(String.format("누락 파일: %s\n", missingId));
            }

            // 2. 각 파일 검증 (단일 파일 메소드 호출)
            for (Path file : jsonFiles) {
                String fromStId = file.getFileName().toString().replace("_routes.json", "");
                validateSingleFile(file, writer, fromStId, allStIds);
            }
        }
    }

    /**
     * 단일 파일의 통근 데이터 건수를 검증하고, 누락 to_st_id 로깅 (stationMap 기준).
     * @param file 검증할 파일 경로
     * @param writer 누락 로그 writer
     * @param fromStId 파일의 from_st_id
     * @param allStIds 전체 st_id Set (stationMap 기준)
     */
    @SneakyThrows
    private void validateSingleFile(Path file, BufferedWriter writer, String fromStId, Set<String> allStIds) {
        List<CommutingAllData> routes = new ArrayList<>();
        try (FileReader reader = new FileReader(file.toFile(), StandardCharsets.UTF_8)) {
            List<CommutingAllData> fileRoutes = gson.fromJson(reader, new TypeToken<List<CommutingAllData>>(){}.getType());
            if (fileRoutes != null) {
                routes.addAll(fileRoutes);
            }
        } catch (Exception e) {
            log.error("파일 읽기 실패: file={}, error={}", file, e.getMessage(), e);
            return;
        }

        // 파일 내 to_st_id Set 추출 (Set<String> 타입 반영)
        Set<String> existingToIds = routes.stream()
                .flatMap(route -> route.getTo_st_id().stream())  // Set<String>을 flatMap으로 펼침
                .collect(Collectors.toSet());

        int actualCount = existingToIds.size();  // 중복 제거 건수 (to_st_id 기준)

        // 예상 to Set: allStIds - self (fromStId 제외)
        Set<String> expectedToIds = new HashSet<>(allStIds);
        expectedToIds.remove(fromStId);

        if (actualCount != EXPECTED_ROUTES_PER_FILE || !existingToIds.equals(expectedToIds)) {
            log.error("파일 데이터 불일치: file={}, 예상={}건, 실제={}건", file.getFileName(), EXPECTED_ROUTES_PER_FILE, actualCount);

            // 누락 to_st_id 계산: expectedToIds - existingToIds
            Set<String> missingToIds = new HashSet<>(expectedToIds);
            missingToIds.removeAll(existingToIds);

            // 누락 로깅
            for (String missingTo : missingToIds) {
                writer.write(String.format("%s: %s 누락\n", file.getFileName(), missingTo));
            }
            writer.flush();
        } else {
            log.info("파일 데이터 검증 성공: file={}, {}건", file.getFileName(), EXPECTED_ROUTES_PER_FILE);
        }
    }

    public List<CommutingAllData> fromCommutingData(Path filePath) {
        List<CommutingAllData> routes =  new ArrayList<>();
        // dataIoService를 통해 JSON 파일 읽기
        try (FileReader reader = new FileReader(filePath.toFile(), StandardCharsets.UTF_8)) {
            List<CommutingAllData> fileRoutes = gson.fromJson(reader, new TypeToken<List<CommutingAllData>>(){}.getType());
            if (fileRoutes != null) {
                routes.addAll(fileRoutes);
            }
            return routes;
        } catch (Exception e) {
            log.error("파일 읽기 실패: file={}, error={}", filePath, e.getMessage());
            return null;
        }
    }

    /**
     *  폴더의 파일 개수 반환 json
     */
    public int countJsonFiles(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) return 0;
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        return files != null ? files.length : 0;
    }

    /**
     *  파일안의 통근 json 체크.
     */
    public void validateCommutingJsonPath(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) return;
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (files == null || files.length == 0) return;

        Gson gson = new Gson();
        int mismatchCount = 0;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                Type listType = new TypeToken<List<CommutingAllData>>(){}.getType();
                List<CommutingAllData> dataList = gson.fromJson(reader, listType);

                for (int i = 0; i < dataList.size(); i++) {
                    CommutingAllData data = dataList.get(i);
                    int pathSize = (data.getPath() != null) ? data.getPath().size() : 0;
                    int stIdSize = (data.getPath_st_ids() != null) ? data.getPath_st_ids().size() : 0;

                    if (pathSize != stIdSize) {
                        mismatchCount++;
                        log.error("❗ 불일치 - 파일: {} | 인덱스: #{} | path: {}개, path_st_ids: {}개", file.getName(), i, pathSize, stIdSize);
                    }
                }
                if(mismatchCount == 0) {
                    esIndexer.indexFolder(dataList, file.getName());
                }
            } catch (Exception e) {
                log.error("파일 읽기 오류: {} - {}", file.getName(), e.getMessage());
            }
        }

        log.info("검사 완료: 총 불일치 파일 수 = {}", mismatchCount);
    }
}
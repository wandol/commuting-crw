package kr.co.saramin.lab.commutingcrw.module;

import kr.co.saramin.lab.commutingcrw.vo.ComData;
import kr.co.saramin.lab.commutingcrw.vo.CommutingData;
import kr.co.saramin.lab.commutingcrw.vo.PersonsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FgfParser {

    public static List<CommutingData> parseCommutingAllFiles(String folderPath) throws IOException {
        List<CommutingData> allRoutes = new ArrayList<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".fgf")) // 확장자에 따라 조정
                .forEach(filePath -> {
                    try {
                        List<CommutingData> routes = parseSingleFileC(filePath.toFile());
                        allRoutes.addAll(routes);
                    } catch (IOException e) {
                        log.error("Failed to read file: {} - {}", filePath, e.getMessage());
                    }
                });

        return allRoutes;
    }

    public static List<PersonsData> parsePersonsAllFiles(String folderPath) throws IOException {
        List<PersonsData> allPersons = new ArrayList<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt")) // 확장자에 따라 조정
                .forEach(filePath -> {
                    try {
                        List<PersonsData> routes = parseSingleFileP(filePath.toFile());
                        allPersons.addAll(routes);
                    } catch (IOException e) {
                        log.error("Failed to read file: {} - {}", filePath, e.getMessage());
                    }
                });

        return allPersons;
    }

    public static List<ComData> parseComAllFiles(String folderPath) throws IOException {
        List<ComData> allCom = new ArrayList<>();

        Files.walk(Paths.get(folderPath))
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt")) // 확장자에 따라 조정
                .forEach(filePath -> {
                    try {
                        List<ComData> routes = parseSingleFileCo(filePath.toFile());
                        allCom.addAll(routes);
                    } catch (IOException e) {
                        log.error("Failed to read file: {} - {}", filePath, e.getMessage());
                    }
                });
        return allCom;
    }

    private static List<CommutingData> parseSingleFileC(File file) throws IOException {
        List<CommutingData> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        Map<String, String> currentMap = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("<__startNodeNmSearch__>")) {
                if (!currentMap.isEmpty()) {
                    result.add(convertToVO(currentMap));
                    currentMap.clear();
                }
            }

            if (line.contains(">")) {
                String[] split = line.split(">", 2);
                if (split.length == 2) {
                    String key = split[0].replaceAll("[<>]", "").trim();
                    String value = split[1].trim();
                    currentMap.put(key, value);
                }
            }
        }

        if (!currentMap.isEmpty()) {
            result.add(convertToVO(currentMap));
        }

        return result;
    }

    private static List<PersonsData> parseSingleFileP(File file) throws IOException {
        List<PersonsData> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        Map<String, String> currentMap = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("<__mem_idx__>")) {
                if (!currentMap.isEmpty()) {
                    result.add(convertToPersonsVO(currentMap));
                    currentMap.clear();
                }
            }

            if (line.contains(">")) {
                String[] split = line.split(">", 2);
                if (split.length == 2) {
                    String key = split[0].replaceAll("[<>]", "").trim();
                    String value = split[1].trim();
                    currentMap.put(key, value);
                }
            }
        }

        if (!currentMap.isEmpty()) {
            result.add(convertToPersonsVO(currentMap));
        }

        return result;
    }

    private static List<ComData> parseSingleFileCo(File file) throws IOException {
        List<ComData> result = new ArrayList<>();
        List<String> lines = Files.readAllLines(file.toPath());

        Map<String, String> currentMap = new HashMap<>();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("<__csn__>")) {
                if (!currentMap.isEmpty()) {
                    result.add(convertToComVO(currentMap));
                    currentMap.clear();
                }
            }

            if (line.contains(">")) {
                String[] split = line.split(">", 2);
                if (split.length == 2) {
                    String key = split[0].replaceAll("[<>]", "").trim();
                    String value = split[1].trim();
                    currentMap.put(key, value);
                }
            }
        }

        if (!currentMap.isEmpty()) {
            result.add(convertToComVO(currentMap));
        }

        return result;
    }

    private static CommutingData convertToVO(Map<String, String> map) {
        CommutingData vo = new CommutingData();

        vo.setStartNodeNmSearch(map.get("__startNodeNmSearch__"));
        vo.setStartStNmSearch(map.get("__startStNmSearch__"));
        vo.setEndCodeSearch(map.get("__endCodeSearch__"));
        vo.setStartStNm(map.get("__startStNm__"));
        vo.setEndStNm(map.get("__endStNm__"));
        vo.setTotalCost(Integer.parseInt(map.getOrDefault("__totalCost__", "0")));

        vo.setPathsNm(splitByComma(map.get("__pathsNm__")));
        vo.setPathsCd(splitByComma(map.get("__pathsCd__")));
        vo.setTransferNode(splitByComma(map.get("__transferNode__")));
        vo.setTransferStNm(splitByComma(map.get("__transferStNm__")));
        vo.setTransferStCd(splitByComma(map.get("__transferStCd__")));

        return vo;
    }

    private static PersonsData convertToPersonsVO(Map<String, String> map) {
        PersonsData vo = new PersonsData();

        vo.setMem_idx(map.get("__mem_idx__"));
        vo.setX_coordinate(map.get("__x_coordinate__"));
        vo.setY_coordinate(map.get("__y_coordinate__"));
        return vo;
    }

    private static ComData convertToComVO(Map<String, String> map) {
        ComData vo = new ComData();

        vo.setCsn(map.get("__csn__"));
        vo.setX_coordinate(map.get("__x_coordinate__"));
        vo.setY_coordinate(map.get("__y_coordinate__"));
        return vo;
    }

    private static List<String> splitByComma(String input) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        return Arrays.stream(input.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }


}
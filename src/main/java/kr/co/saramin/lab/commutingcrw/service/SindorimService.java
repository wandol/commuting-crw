package kr.co.saramin.lab.commutingcrw.service;

import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.module.FgfParser;
import kr.co.saramin.lab.commutingcrw.module.SindorimCase;
import kr.co.saramin.lab.commutingcrw.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SindorimService {

    private final Environment environment;
    private final String NEAR_BY_ST = "신도림";
    private final int LIMIT_HOUR = 30;
    private final SindorimCase sindorimCase;

    public void sindorimP() {

        //  1. 회원데이터 txt 파일 가져오기
        //  2. 통근데이터 가져오기
        //  3. 통근데이터에서 신도림역이면서 60분 이내인 역리스트업.
        try {
            /* ===================== metro_persons data ===================== */
            Global.PERSON_DATA = FgfParser.parsePersonsAllFiles(environment.getProperty("sindorim.persons")).stream()
                    .filter(p -> StringUtils.hasText(p.getMem_idx()))
                    .collect(Collectors.toConcurrentMap(
                            PersonsData::getMem_idx,  // key mapper
                            Function.identity(),      // value mapper
                            (existing, replacement) -> existing // key 중복 시 기존 값 유지
                    ));
            log.info("persons size : {}", Global.PERSON_DATA.size());

//            /* ===================== metro_commuting data ===================== */
//            Global.COMMUTING_DATA = FgfParser.parseCommutingAllFiles(environment.getProperty("sindorim.commuting"));
//            log.info("commutingData size : {}", Global.COMMUTING_DATA.size());
//
//            /* ===================== companyinfo_search data ===================== */
//            Global.COM_DATA = FgfParser.parseComAllFiles(environment.getProperty("sindorim.com")).stream()
//                    .filter(p -> StringUtils.hasText(p.getCsn()))
//                    .filter(p -> StringUtils.hasText(p.getX_coordinate()))
//                    .filter(p -> StringUtils.hasText(p.getY_coordinate()))
//                    .collect(Collectors.toConcurrentMap(
//                            ComData::getCsn,  // key mapper
//                            Function.identity(),      // value mapper
//                            (existing, replacement) -> existing // key 중복 시 기존 값 유지
//                    ));
//
//            log.info("com data size : {}", Global.COM_DATA.size());
//
//            /* ===================== sindorimNearBy data ===================== */
//            List<String> GTX_B = Arrays.asList("송도","인천시청","부평","부천종합운동장","여의도","용산","서울역","청량리","상봉","별내","평내호평","마석");
//            List<CommutingData> sindorimNearBy = Global.COMMUTING_DATA.stream()
//                    .filter(Objects::nonNull)
//                    .filter(commuting -> commuting.getStartStNmSearch().equals(NEAR_BY_ST))
//                    .filter(d
//                            -> (d.getTotalCost() <= LIMIT_HOUR && d.getTransferStCd().size() <= 1)
//                            || (d.getTotalCost() <= 60 && d.getEndCodeSearch().startsWith("102") || d.getEndCodeSearch().startsWith("101"))
//                            || (GTX_B.contains(d.getEndStNm()))
//                    )
//                    .collect(Collectors.toList());

            /* ===================== target mem data ===================== */
//            List<TargetMember> targetMembersIncludeCsn = targetMember(environment.getProperty("sindorim.targetMember"));
//            log.info("targetMembersIncludeCsn size : {}",targetMembersIncludeCsn.size());
//            List<TargetMember> targetMembersExcludeCsn = targetMember(environment.getProperty("sindorim.no-targetMember"));
//            log.info("targetMembersExcludeCsn size : {}",targetMembersExcludeCsn.size());

//            SindorimCase.fileWriteForMap(targetMembersIncludeCsn,"/Users/user/commuting/map/com_vs_sindorim.json");
            //  case1 회원들중  신도림역에서 1시간정도 소요되는 1&2호선 역 근처에 사는 인재 수요
//            SindorimCase.case1(sindorimNearBy, targetMembersIncludeCsn);
            //  case2 회원들중  현재 혹은 최근 기업의 통근시간 비교
//            SindorimCase.case2(targetMembersIncludeCsn);
            //  case3 기업과 신도림
//            SindorimCase.case3();
            SindorimCase.comAll();
//            SindorimCase.memAll();
            //  apply mem target
//            SindorimCase.case4();
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

    }


    @SneakyThrows
    private List<TargetMember> targetMember(String filePath){
        List<TargetMember> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue; // 빈 줄 스킵
                result.add(parseCsvLine(line));
            }
        }
        return result;
    }

    public TargetMember parseCsvLine(String line) {
        String[] tokens = line.split(",", -1); // 빈 칸도 유지

        TargetMember vo = new TargetMember();
        vo.setMem_idx(tokens[0]);
        vo.setCsn(tokens[1]);

        vo.setUp1(splitToList(tokens[2]));
        vo.setUp1_nm(splitToList(tokens[3]));
        vo.setUp2(splitToList(tokens[4]));
        vo.setUp2_nm(splitToList(tokens[5]));
        vo.setUp3(splitToList(tokens[6]));
        vo.setUp3_nm(splitToList(tokens.length > 7 ? tokens[7] : ""));

        return vo;
    }

    private List<String> splitToList(String value) {
        if (value == null || value.isBlank()) return new ArrayList<>();
        return Arrays.asList(value.split("\\|"));
    }

//    @PostConstruct
    @SneakyThrows
    private void loadMetroData(){
        log.info("==== dictionary subway-info-data ====");
        Path subwayInfo = Paths.get(Objects.requireNonNull(environment.getProperty("sindorim.metro-info")));
        Global.METRO_INFO_DATA = Files.readAllLines(subwayInfo).stream()
                .map(s -> s.split("\\|",12))
                .map(SindorimService::apply)
                .collect(Collectors.toList());

        log.info("==== dictionary sri-subway-data ====");
        Path sriSubway = Paths.get(Objects.requireNonNull(environment.getProperty("sindorim.metro-sri")));
        Global.METRO_SRI_DATA = Files.readAllLines(sriSubway).stream()
                .map(s -> s.split(","))
                .filter(strings -> strings.length == 2)
                .collect(Collectors.toConcurrentMap(s -> s[0],s2-> s2[1].trim(), (s, s2) -> s));
    }

    private static MetroDataVO apply(String[] ss) {
        return MetroDataVO.builder()
                .node_id(ss[0])
                .node_nm(ss[1])
                .st_id(ss[2])
                .st_nm(ss[3])
                .gps_x(ss[4])
                .gps_y(ss[5])
                .gps_x_real(ss[6])
                .gps_y_real(ss[7])
                .trans_type(ss[8])
                .area_nm(ss[9])
                .sri_subway_cd(ss[10])
                .dis(0.0)
                .build();
    }


//    public static void writeMembersToCsv(String filePath, List<SindorimOutputVO> result) {
//        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
//            // CSV 헤더
//            writer.write("memIdx,csn,commutingMinToCom,commutingMinToSindorim");
//            writer.newLine();
//            // 데이터 작성
//            for (SindorimOutputVO output : result) {
//                if("ok".equals(output.getMsg())){
//                    String line = String.format("%s,%s,%d,%d",
//                            output.getMemIdx(),output.getCsn(),output.getCommutingMinToCom(), output.getCommutingMinToSindorim());
//                    writer.write(line);
//                    writer.newLine();
//                }
//            }
//
//            writer.flush();
//        } catch (IOException e) {
//            throw new RuntimeException("CSV 파일 작성 중 오류 발생", e);
//        }
//    }
}

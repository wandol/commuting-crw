package kr.co.saramin.lab.commutingcrw.module;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class SindorimCase {

    private static final int LIMIT_NEARBY_DISTANCE = 2000;
    private static final String SINDORIM = "신도림";

    /**
     *  1. 회원의 좌표값 여부 확인
     *  2. 회원 좌표값으로 근처 지하철역 확인
     *  3. 근처 지하철역
     * @param sindorimNearByOneHour     신도림 1시간 이내 지하철역 데이터
     * @param targetMembersIncludeCsn   회원데이터.
     */
    public static void case1(List<CommutingData> sindorimNearByOneHour, List<TargetMember> targetMembersIncludeCsn) {
        if(targetMembersIncludeCsn.isEmpty()) {
            log.error("targetMembersIncludeCsn is empty");
            System.exit(1);
        }
        ConcurrentMap<String, CommutingData> sindorimNearStData = sindorimNearByOneHour.stream().collect(Collectors.toConcurrentMap(
                CommutingData::getEndStNm,  // key mapper
                Function.identity(),      // value mapper
                (existing, replacement) -> existing // key 중복 시 기존 값 유지
        ));
        ForkJoinPool forkJoinPool = new ForkJoinPool(6);
        ConcurrentLinkedDeque<TargetMember> resultQueue = new ConcurrentLinkedDeque<>();
        AtomicInteger count = new AtomicInteger(0);
        int total = targetMembersIncludeCsn.size();
        try{
            forkJoinPool.submit(() -> {
                IntStream.range(0, total).parallel().forEach(i -> {
                    TargetMember targetMember = targetMembersIncludeCsn.get(i);
                    if (Global.PERSON_DATA.containsKey(targetMember.getMem_idx())) {
                        PersonsData personsData = Global.PERSON_DATA.get(targetMember.getMem_idx());
                        // 좌표값이 있는 경우
                        if (StringUtils.hasText(personsData.getX_coordinate())) {
                            //  회원의 근처 지하철역 찾기
                            MetroDataVO nearBySt = getNearBySt(personsData.getY_coordinate(), personsData.getX_coordinate());
                            if (nearBySt != null) {
                                if (sindorimNearStData.containsKey(nearBySt.getSt_nm())) {
                                    targetMember.setSt_nm(nearBySt.getSt_nm());
                                    resultQueue.add(targetMember);
                                }
                            }
                        }
                    }
                    int current = count.incrementAndGet();
                    if (current % 10000 == 0 || current == total) {
                        log.info("진행사항 : {}/{} ({}%)", current, total, current * 100 / total);
                    }
                });
            }).get();
        }catch (Exception e){
            log.error("case1 fail");
            Thread.currentThread().interrupt();
        }finally {
            forkJoinPool.shutdown();
        }

        log.info("memList size : {}", resultQueue.size());
        List<TargetMember> memList = new ArrayList<>(resultQueue);
//        writeMembersToCsv("/Users/user/commuting/mem_commuting_list_no_csn.csv", memList);
        writeMembersToCsv("/Users/user/commuting/mem_commuting_list.csv", memList);
        log.info("done!");
        System.exit(0);
    }

    public static void case2(List<TargetMember> targetMembers) {
        if(targetMembers.isEmpty()) {
            log.error("targetMembers is empty");
            System.exit(1);
        }

        ForkJoinPool forkJoinPool = new ForkJoinPool(6);
                    ConcurrentLinkedDeque<CommtingMember> resultQueue = new ConcurrentLinkedDeque<>();
                    AtomicInteger count = new AtomicInteger(0);
                    int total = targetMembers.size();
                    try{
                        forkJoinPool.submit(() -> {
                            IntStream.range(0, total).parallel().forEach(i -> {
                                TargetMember targetMember = targetMembers.get(i);
                    if (Global.PERSON_DATA.containsKey(targetMember.getMem_idx())
                    && Global.COM_DATA.containsKey(targetMember.getCsn()) ) {
                        //  회원데이터.
                        PersonsData personsData = Global.PERSON_DATA.get(targetMember.getMem_idx());
                        //  기업데이터
                        ComData comData = Global.COM_DATA.get(targetMember.getCsn());

                        // 좌표값이 있는 경우
                        if (StringUtils.hasText(personsData.getX_coordinate())
                                && StringUtils.hasText(comData.getX_coordinate())) {
                            //  회원의 근처 지하철역 찾기
                            MetroDataVO nearBySt = getNearBySt(personsData.getY_coordinate(), personsData.getX_coordinate());
                            //  기업의 근처 지하철역 찾기
                            MetroDataVO comNearBySt = getNearBySt(comData.getY_coordinate(), comData.getX_coordinate());
                            //  신도림
                            MetroDataVO sindorim = Global.METRO_INFO_DATA.stream().filter(metroDataVO -> metroDataVO.getSt_nm().equals(SINDORIM))
                                    .findFirst().orElse(null);

                            if(Objects.nonNull(nearBySt) && Objects.nonNull(comNearBySt)) {
                                int toCom = 0;
                                int toSindorim = 0;
                                CommutingData commutingDataByCom = null;
                                CommutingData commutingDataBySindorim = null;
                                        //  신도림
                                if(!comNearBySt.getSt_nm().equals(nearBySt.getSt_nm())) {
                                    // 회원/기업 간 통근시간,  회원/심도림 간 통근시간.
                                    commutingDataByCom = Global.COMMUTING_DATA.stream()
                                            .filter(c ->
                                                    (c.getStartStNm().equals(nearBySt.getSt_nm()) && c.getStartNodeNmSearch().equals(nearBySt.getNode_nm()))
                                                            &&
                                                            (c.getEndStNm().equals(comNearBySt.getSt_nm()) || c.getEndCodeSearch().equals(comNearBySt.getSri_subway_cd()))
                                            )
                                            .findFirst().orElse(null);
                                    toCom = commutingDataByCom != null ? commutingDataByCom.getTotalCost() : 11111;
                                }
                                if(!sindorim.getSt_nm().equals(nearBySt.getSt_nm())) {
                                    commutingDataBySindorim = Global.COMMUTING_DATA.stream()
                                            .filter(c ->
                                                    (c.getStartStNm().equals(nearBySt.getSt_nm()))
                                                            &&
                                                            (c.getEndStNm().equals(sindorim.getSt_nm()) || c.getEndCodeSearch().equals(sindorim.getSri_subway_cd()))
                                            )
                                            .findFirst().orElse(null);
                                    toSindorim = commutingDataBySindorim != null ? commutingDataBySindorim.getTotalCost() : 11111;
                                }

                                if(Objects.nonNull(commutingDataBySindorim) &&  Objects.nonNull(commutingDataByCom)) {
                                    CommtingMember result = new CommtingMember();
                                    result.setCsn(targetMember.getCsn());
                                    result.setMem_idx(targetMember.getMem_idx());
                                    result.setCommuting_com(toCom);
                                    result.setCommuting_sindorim(toSindorim);
                                    resultQueue.add(result);
                                }
                            }
                        }
                    }
                    int current = count.incrementAndGet();
                    if (current % 10000 == 0 || current == total) {
                        log.info("진행사항 : {}/{} ({}%)", current, total, current * 100 / total);
                    }
                });
            }).get();
        }catch (Exception e){
            log.error("case2 fail");
            Thread.currentThread().interrupt();
        }finally {
            forkJoinPool.shutdown();
        }

        log.info("memList size : {}", resultQueue.size());
        List<CommtingMember> memList = new ArrayList<>(resultQueue);
        writeMembersToCsvCommuting("/Users/user/commuting/commuting_memToCom_memToSindorim.csv", memList);
        log.info("done!");
        System.exit(0);
    }

    public static void case3() {
        List<String> result = new ArrayList<>();
        List<ComData> comList = Global.COM_DATA.entrySet().stream()
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        ForkJoinPool forkJoinPool = new ForkJoinPool(6);
        ConcurrentLinkedDeque<String> resultQueue = new ConcurrentLinkedDeque<>();
        AtomicInteger count = new AtomicInteger(0);
        int total = comList.size();
        try{
            forkJoinPool.submit(() -> {
                IntStream.range(0, total).parallel().forEach(i -> {
                    ComData targetMember = comList.get(i);
                    if(!targetMember.getX_coordinate().isEmpty()){
                        MetroDataVO comNearBySt = getNearBySt(targetMember.getY_coordinate(), targetMember.getX_coordinate());
                        if(comNearBySt != null) {
                            //  신도림
                            MetroDataVO sindorim = Global.METRO_INFO_DATA.stream().filter(metroDataVO -> metroDataVO.getSt_nm().equals(SINDORIM))
                                    .findFirst().orElse(null);
                            CommutingData commutingDataBySindorim = Global.COMMUTING_DATA.stream()
                                    .filter(c ->
                                            (c.getStartStNm().equals(comNearBySt.getSt_nm()))
                                                    &&
                                                    (c.getEndStNm().equals(sindorim.getSt_nm()) || c.getEndCodeSearch().equals(sindorim.getSri_subway_cd()))
                                    )
                                    .findFirst().orElse(null);
                            if(commutingDataBySindorim != null) {
                                resultQueue.add(targetMember.getCsn() + "," + commutingDataBySindorim.getTotalCost());
                            }
                        }
                    }
                    int current = count.incrementAndGet();
                    if (current % 10000 == 0 || current == total) {
                        log.info("진행사항 : {}/{} ({}%)", current, total, current * 100 / total);
                    }
                });
            }).get();
        }catch (Exception e){
            log.error("case3 fail");
            Thread.currentThread().interrupt();
        }finally {
            forkJoinPool.shutdown();
        }
        List<String> comBySin = new ArrayList<>(resultQueue);
        saveJsonToFile(String.join("\n", comBySin),"/Users/user/commuting/com_sindorim.csv");
    }

    /**
     *  근처 지하철역 찾기.
     */
    public static MetroDataVO getNearBySt(String lat1, String lon1){
        List<MetroDataVO> result = new ArrayList<>();
        //  회원의 근처 지하철역 찾기
        for (MetroDataVO metroDatum : Global.METRO_INFO_DATA) {
            double dis = getVincentyDistance(
                    Double.parseDouble(lat1), Double.parseDouble(lon1)
                    , Double.parseDouble(metroDatum.getGps_y_real()), Double.parseDouble(metroDatum.getGps_x_real())
            );
            if(dis <= LIMIT_NEARBY_DISTANCE ){
                metroDatum.setDis(dis);
                result.add(metroDatum);
            }
        }
        if(result.isEmpty()) return null;
        return result.stream().min(Comparator.comparingDouble(MetroDataVO::getDis)).get();
    }

    public static void comAll() throws IOException {

//        List<ComData> comList = FgfParser.parseComAllFiles("/Users/user/commuting/companyinfo_search");
//        List<MetroDataVO> result = new ArrayList<>();
//        int count = 0;
//        int total = comList.size();
//        for (ComData comData : comList) {
//            if(!comData.getX_coordinate().isEmpty()) {
//                MetroDataVO comNearBySt = getNearBySt(comData.getY_coordinate(), comData.getX_coordinate());
//                if(comNearBySt != null) {
//                    comNearBySt.setCsn(comData.getCsn());
//                    result.add(comNearBySt);
//                }
//            }
//            count++;
//            if (count % 10000 == 0 || count == total) {
//                log.info("진행사항 : {}/{} ({}%)", count, total, count * 100 / total);
//                break;
//            }
//        }

        List<PersonsData> memList = Global.PERSON_DATA.entrySet().stream()
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        saveJsonToFile(memList.stream()
                .filter(metroDataVO -> !metroDataVO.getX_coordinate().isEmpty())
                .map(metroDataVO -> metroDataVO.getMem_idx() + "|" + metroDataVO.getX_coordinate() + "|" + metroDataVO.getY_coordinate())
                .collect(Collectors.joining("\n")),"/Users/user/commuting/mem_all.csv");
//        ForkJoinPool forkJoinPool = new ForkJoinPool(6);
//        ConcurrentLinkedDeque<MetroDataVO> resultQueue = new ConcurrentLinkedDeque<>();
//        AtomicInteger count = new AtomicInteger(0);
//        int total = comList.size();
//        try{
//            forkJoinPool.submit(() -> {
//                IntStream.range(0, total).forEach(i -> {
//                    ComData targetMember = comList.get(i);
//                    if(!targetMember.getX_coordinate().isEmpty()) {
//                        MetroDataVO comNearBySt = getNearBySt(targetMember.getY_coordinate(), targetMember.getX_coordinate());
//                        if(comNearBySt != null) {
//                            comNearBySt.setCsn(targetMember.getCsn());
//                            resultQueue.add(comNearBySt);
//                        }
//                    }
//                    int current = count.incrementAndGet();
//                    if (current % 10000 == 0 || current == total) {
//                        log.info("진행사항 : {}/{} ({}%)", current, total, current * 100 / total);
//                    }
//                });
//            }).get();
//        }catch (Exception e){
//            e.printStackTrace();
//            log.error("comAll fail");
//            Thread.currentThread().interrupt();
//        }finally {
//            forkJoinPool.shutdown();
//        }
//        List<MetroDataVO> comAll = new ArrayList<>(resultQueue);
//        saveJsonToFile(result.stream()
//                .map(metroDataVO -> metroDataVO.getCsn() + "|" + metroDataVO.getGps_x_real() + "|" + metroDataVO.getGps_y_real())
//                .collect(Collectors.joining("\n")),"/Users/user/commuting/com_all.csv");
    }

    public static void memAll() {
        List<PersonsData> memList = Global.PERSON_DATA.entrySet().stream()
                .filter(Objects::nonNull)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        int total = memList.size();
        int threadCount = 6;
        ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
        AtomicInteger count = new AtomicInteger(0);

        String dirPath = "/Users/user/commuting/mem_all/";

        try {
            forkJoinPool.submit(() ->
                    IntStream.range(0, total).parallel().forEach(i -> {
                        PersonsData targetMember = memList.get(i);
                        if (!targetMember.getX_coordinate().isEmpty()) {
                            MetroDataVO comNearBySt = getNearBySt(targetMember.getY_coordinate(), targetMember.getX_coordinate());
                            if (comNearBySt != null) {
                                comNearBySt.setMem_idx(targetMember.getMem_idx());
                                // 현재 쓰레드 ID로 파일명 결정
                                String fileName = String.format("mem_%s.csv", Thread.currentThread().getName());
                                String filePath = dirPath + fileName;

                                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                                    writer.write(comNearBySt.toMemString());
                                    writer.newLine();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    log.error("파일 쓰기 실패 - {}", fileName);
                                }
                            }
                        }

                        int current = count.incrementAndGet();
                        if (current % 10000 == 0 || current == total) {
                            log.info("진행사항 : {}/{} ({}%)", current, total, current * 100 / total);
                        }
                    })
            ).get();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("memAll fail");
            Thread.currentThread().interrupt();
        } finally {
            forkJoinPool.shutdown();
        }
    }

    public static double getVincentyDistance(double lat1, double lon1, double lat2, double lon2) {
        double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563;
        double L = Math.toRadians(lon2 - lon1);
        double U1 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - f) * Math.tan(Math.toRadians(lat2)));
        double sinU1 = Math.sin(U1), cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2), cosU2 = Math.cos(U2);
        double cosSqAlpha, sinSigma, cos2SigmaM, cosSigma, sigma;
        double lambda = L, lambdaP, iterLimit = 100;
        do {
            double sinLambda = Math.sin(lambda), cosLambda = Math.cos(lambda);
            sinSigma = Math.sqrt( (cosU2 * sinLambda)
                    * (cosU2 * sinLambda)
                    + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
                    * (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda)
            );
            if (sinSigma == 0) return 0;
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda;
            sigma = Math.atan2(sinSigma, cosSigma);
            double sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma;
            cosSqAlpha = 1 - sinAlpha * sinAlpha;
            cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;
            double C = f / 16 * cosSqAlpha * (4 + f * (4 - 3 * cosSqAlpha));
            lambdaP = lambda;
            lambda =  L + (1 - C) * f * sinAlpha
                    * (sigma + C * sinSigma
                    * (cos2SigmaM + C * cosSigma
                    *(-1 + 2 * cos2SigmaM * cos2SigmaM)
            )
            );

        } while (Math.abs(lambda - lambdaP) > 1e-12 && --iterLimit > 0);
        if (iterLimit == 0) return 0;
        double uSq = cosSqAlpha * (a * a - b * b) / (b * b);
        double A = 1 + uSq / 16384
                * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));
        double deltaSigma =
                B * sinSigma
                        * (cos2SigmaM + B / 4
                        * (cosSigma
                        * (-1 + 2 * cos2SigmaM * cos2SigmaM) - B / 6 * cos2SigmaM
                        * (-3 + 4 * sinSigma * sinSigma)
                        * (-3 + 4 * cos2SigmaM * cos2SigmaM)));
        double s = b * A * (sigma - deltaSigma);
        return s;
    }

    @SneakyThrows
    public static void writeMembersToCsv(String filePath, List<TargetMember> result) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // CSV 헤더
            writer.write("memIdx,csn,up1_cd,up1_nm,up2_cd,up2_nm,up3_cd,up3_nm,st_nm");
            writer.newLine();
            // 데이터 작성
            for (TargetMember output : result) {
                String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        output.getMem_idx(),
                        output.getCsn(),
                        Strings.join(output.getUp1(),'|'),
                        Strings.join(output.getUp1_nm(),'|'),
                        Strings.join(output.getUp2(),'|'),
                        Strings.join(output.getUp2_nm(),'|'),
                        Strings.join(output.getUp3(),'|'),
                        Strings.join(output.getUp3_nm(),'|'),
                        output.getSt_nm());
                writer.write(line);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("CSV 파일 작성 중 오류 발생", e);
        }
    }

    @SneakyThrows
    public static void writeMembersToCsvCommuting(String filePath, List<CommtingMember> result) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // CSV 헤더
            writer.write("memIdx,csn,toCom,toSindorim");
            writer.newLine();
            // 데이터 작성
            for (CommtingMember output : result) {
                String line = String.format("%s,%s,%d,%d",
                        output.getMem_idx(),
                        output.getCsn(),
                        output.getCommuting_com(),
                        output.getCommuting_sindorim());
                writer.write(line);
                writer.newLine();
            }

            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("CSV 파일 작성 중 오류 발생", e);
        }
    }

    public static void fileWriteForMap(List<TargetMember> targetMembersIncludeCsn, String filePath) {
        Map<String, CompanyByMemberData> groupedData = new HashMap<>();

        for (TargetMember targetMember : targetMembersIncludeCsn) {
            ComData comData = Global.COM_DATA.get(targetMember.getCsn());
            if (comData == null) continue;

            String csn = comData.getCsn();
            CompanyByMemberData companyByMemberData = groupedData.computeIfAbsent(csn, k -> {
                CompanyByMemberData data = new CompanyByMemberData();
                data.setCsn(csn);
                data.setX_coordinate(comData.getX_coordinate());
                data.setY_coordinate(comData.getY_coordinate());
                data.setPersonsData(new ArrayList<>()); // 초기화
                return data;
            });

            PersonsData person = Global.PERSON_DATA.get(targetMember.getMem_idx());
            if (person != null) {
                companyByMemberData.getPersonsData().add(person);
            }
        }

        List<CompanyByMemberData> realData = new ArrayList<>(groupedData.values());
        String json = convertToJson(realData);
        log.info("write start");
        saveJsonToFile(json, filePath);
    }

    public static String convertToJson(Object obj) {
        if (obj == null) return null;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveJsonToFile(String json, String filePath) {
        if (json == null || filePath == null) return;
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void case4_streamingGroupBy() {
        String inputFilePath = "/Users/user/commuting/raw/mem_csn.csv";
        String outputBasePath = "/Users/user/commuting/temp/";

        AtomicInteger counter = new AtomicInteger(0);

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFilePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                String csn = parts[1].trim();
                String memIdx = parts[0].trim();

                int bucket;
                try {
                    bucket = Integer.parseInt(csn) % 500;
                } catch (NumberFormatException e) {
                    bucket = Math.abs(csn.hashCode()) % 500;
                }

                String bucketDirPath = outputBasePath + bucket + "/";
                File bucketDir = new File(bucketDirPath);
                if (!bucketDir.exists()) bucketDir.mkdirs();

                String filePath = bucketDirPath + csn + ".csv";

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                    writer.write(memIdx);
                    writer.newLine();
                } catch (IOException e) {
                    log.error("파일 쓰기 실패: {}", filePath, e);
                }

                int current = counter.incrementAndGet();
                if (current % 100000 == 0) {
                    log.info("진행 상황: {}건 처리됨", current);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            log.error("입력 파일 처리 실패", e);
        }

        log.info("총 {}건 처리 완료", counter.get());
    }





    public static void case4() {
        case4_streamingGroupBy();
        List<TargetMember> applyList = loadTargetMembersFromCsnFiles("/Users/user/commuting/temp/");
        fileWriteForMap(applyList, "/Users/user/commuting/map/com_vs_sindorim_apply.json");
    }


    public static List<TargetMember> loadTargetMembersFromCsnFiles(String baseDirPath) {
        List<TargetMember> result = new ArrayList<>();

        for (int i = 0; i < 500; i++) {
            File subDir = new File(baseDirPath + "/" + i);
            if (!subDir.exists() || !subDir.isDirectory()) continue;

            File[] files = subDir.listFiles((dir, name) -> name.endsWith(".csv"));
            if (files == null) continue;

            for (File file : files) {
                String csn = file.getName().replace(".csv", "");

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String memIdx = line.trim();
                        if (memIdx.isEmpty()) continue;

                        TargetMember tm = new TargetMember();
                        tm.setCsn(csn);
                        tm.setMem_idx(memIdx);
                        result.add(tm);
                    }
                } catch (IOException e) {
                    log.error("파일 읽기 실패: {}", file.getAbsolutePath(), e);
                }
            }
        }

        return result;
    }


    public void fileWrite(String mergeDataPath, List<MetroDataVO> m) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(mergeDataPath))) {
            m.forEach(line -> {
                try {
                    writer.write(line.toString());
                    writer.newLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
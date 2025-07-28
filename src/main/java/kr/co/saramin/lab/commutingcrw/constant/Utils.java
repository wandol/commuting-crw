package kr.co.saramin.lab.commutingcrw.constant;

import kr.co.saramin.lab.commutingcrw.vo.MetroAllDataVO;
import kr.co.saramin.lab.commutingcrw.vo.MetroDataVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class Utils {

    /**
     * 이름 매핑
     * 지하철역 변경 또는 이름이 다른 역 매핑
     * @param stNm
     * @return
     */
    public String checkStNm(String stNm) {
        if(stNm.equals("총신대입구(이수)")) return "총신대입구";
        if(stNm.equals("전대∙에버랜드")) return "전대.에버랜드";
        if(stNm.equals("시청∙용인대")) return "시청.용인대";
        if(stNm.equals("운동장∙송담대")) return "용인중앙시장";
        if(stNm.equals("쌍용(나사렛대)")) return "쌍용";
        if(stNm.equals("경기도청 북부청사")) return "경기도청북부청사";
        if(stNm.equals("4∙19민주묘지")) return "4·19민주묘지";
        return stNm;
    }

    /**
     *  지하철 호선명 수정
     *  외부 통근경로와 사람인 호선명이 달라 외부 통근경로명으로 변경
     * @param node_nm
     * @return
     */
    public String checkSriNodeNm(String node_nm) {
        if(node_nm.contains("서울 ")) return node_nm.replace("서울 ", "");
        if(node_nm.contains("에버라인")) return "용인경전철";
        if(node_nm.contains("인천 1호선")) return "인천1호선";
        if(node_nm.contains("인천 2호선")) return "인천2호선";
        if(node_nm.contains("우이신설선")) return "우이신설경전철";
        if(node_nm.contains("김포골드")) return "김포도시철도";
        return node_nm;
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

    public void fileWriteAll(String mergeDataPath, List<MetroAllDataVO> m) {
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

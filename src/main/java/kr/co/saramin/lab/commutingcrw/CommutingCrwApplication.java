package kr.co.saramin.lab.commutingcrw;

import kr.co.saramin.lab.commutingcrw.constant.Global;
import kr.co.saramin.lab.commutingcrw.service.MetroService;
import kr.co.saramin.lab.commutingcrw.service.SindorimService;
import kr.co.saramin.lab.commutingcrw.service.SubwayService;
import kr.co.saramin.lab.commutingcrw.vo.MetroVO;
import kr.co.saramin.lab.commutingcrw.vo.ResultVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class CommutingCrwApplication implements CommandLineRunner {

    private final MetroService metroService;

    public static void main(String[] args) {
        SpringApplication.run(CommutingCrwApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // 지하철 raw data 생성
        metroService.makeRawData();
        System.exit(0);
    }
}

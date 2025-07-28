package kr.co.saramin.lab.commutingcrw.service;

import kr.co.saramin.lab.commutingcrw.module.MakeRawData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MetroService {

    private final MakeRawData makeRawData;

    public void makeRawData() {
        makeRawData.all();
    }

}

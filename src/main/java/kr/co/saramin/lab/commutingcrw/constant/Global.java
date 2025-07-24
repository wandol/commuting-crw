package kr.co.saramin.lab.commutingcrw.constant;

import kr.co.saramin.lab.commutingcrw.vo.ComData;
import kr.co.saramin.lab.commutingcrw.vo.CommutingData;
import kr.co.saramin.lab.commutingcrw.vo.MetroDataVO;
import kr.co.saramin.lab.commutingcrw.vo.PersonsData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Global {
    public final static ConcurrentHashMap<String,String> SRI_CODE_MAP = new ConcurrentHashMap<>();

    public final static ConcurrentHashMap<String,String> LINE_MAP = new ConcurrentHashMap<>();

    public static List<MetroDataVO> METRO_INFO_DATA = new ArrayList<>();
    public static ConcurrentMap<String, String> METRO_SRI_DATA = new ConcurrentHashMap<>();
    public static ConcurrentMap<String,PersonsData> PERSON_DATA = new ConcurrentHashMap<>();
    public static ConcurrentMap<String,ComData> COM_DATA = new ConcurrentHashMap<>();
    public static List<CommutingData> COMMUTING_DATA = new ArrayList<>();
}
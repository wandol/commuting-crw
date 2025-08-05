package kr.co.saramin.lab.commutingcrw.constant;

import lombok.Getter;

@Getter
public enum Region {
    busan("4"),
    daegu("5"),
    gwangju("6"),
    daejeon("8"),
    metro("metro");  // 그 외 모든 경우

    private final String prefix;

    Region(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 주어진 node_id의 접두사를 기반으로 Region을 결정.
     * @param nodeId node_id (e.g., "101", "401")
     * @return 해당 Region
     */
    public static Region fromNodeId(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return metro;
        }
        switch (nodeId.charAt(0)) {
            case '4':
                return busan;
            case '5':
                return daegu;
            case '6':
                return gwangju;
            case '8':
                return daejeon;
            default:
                return metro;
        }
    }

}
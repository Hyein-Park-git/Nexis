package com.example.nexis.agent.core;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.OperatingSystem;
import java.net.InetAddress;

// OSHI 라이브러리를 사용해 실제 시스템 정보를 수집하는 클래스
// OSHI(Operating System and Hardware Information): JVM에서 OS/하드웨어 정보를 가져오는 크로스플랫폼 라이브러리
public class SystemInfoCollector {

    // SystemInfo는 무거운 객체이므로 한 번만 생성해 공유 (static)
    private static final SystemInfo systemInfo = new SystemInfo();

    // CPU 측정은 순간값이 아닌 두 시점의 틱 차이로 계산하므로, 마지막 측정값을 캐싱
    private static double lastCpuUsage = 0;

    // CPU 사용률 측정 — 1초 간격으로 두 번 틱을 읽어 차이로 계산 (순간값보다 정확)
    public static double getCpuUsage() {
        try {
            CentralProcessor processor = systemInfo.getHardware().getProcessor();
            long[] prevTicks = processor.getSystemCpuLoadTicks(); // 현재 틱 스냅샷
            Thread.sleep(1000);                                    // 1초 대기
            lastCpuUsage = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100; // 0.0~1.0 → %
        } catch (InterruptedException ignored) {}
        return lastCpuUsage;
    }

    // 전체 물리 메모리 (bytes)
    public static long getTotalMemory() {
        return systemInfo.getHardware().getMemory().getTotal();
    }

    // 사용 가능한 메모리 (bytes) — OS가 실제로 사용 가능하다고 보고하는 값
    public static long getFreeMemory() {
        return systemInfo.getHardware().getMemory().getAvailable();
    }

    // 메모리 사용률 (%) = (전체 - 여유) / 전체 * 100
    public static double getMemoryUsagePercent() {
        long total = getTotalMemory();
        long free  = getFreeMemory();
        return ((double)(total - free) / total) * 100;
    }

    // OSHI로 호스트명 조회 — 실패 시 "UNKNOWN-PC"
    public static String getHostname() {
        try {
            return systemInfo.getOperatingSystem()
                    .getNetworkParams().getHostName();
        } catch (Exception e) {
            return "UNKNOWN-PC";
        }
    }

    // OS 이름 + 버전 + 빌드번호 조합 (예: "Windows 10.0 19045")
    public static String getOSName() {
        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            String family  = os.getFamily();
            String version = os.getVersionInfo().getVersion();
            String build   = os.getVersionInfo().getBuildNumber();
            return family + " " + version + " " + build;
        } catch (Exception e) {
            return "UNKNOWN-OS";
        }
    }

    // 외부 통신에 사용되는 IP 주소 조회
    // 루프백(127.x.x.x) 제외, IPv4 우선, 첫 번째 인터페이스 선택
    public static String getIpAddress() {
        try {
            return systemInfo.getHardware().getNetworkIFs().stream()
                .filter(n -> n.getIPv4addr().length > 0)
                .filter(n -> !n.getIPv4addr()[0].startsWith("127."))
                .findFirst()
                .map(n -> n.getIPv4addr()[0])
                .orElse(InetAddress.getLocalHost().getHostAddress()); // 못 찾으면 기본 주소
        } catch (Exception e) {
            return "";
        }
    }

    // 모든 지표를 수집해 JSON 문자열로 반환
    // agentNameOverride: agent.properties에 agent.name이 있으면 호스트명 대신 사용
    public static String collect(String agentNameOverride) {
        double cpu       = getCpuUsage();
        long   totalMem  = getTotalMemory();
        long   freeMem   = getFreeMemory();
        double memUsage  = getMemoryUsagePercent();
        String hostname  = (agentNameOverride != null && !agentNameOverride.isBlank())
                           ? agentNameOverride
                           : getHostname();
        String osName    = getOSName();
        String ipAddress = getIpAddress();

        String json = String.format(
            "{ \"hostname\": \"%s\", \"cpuUsage\": %.2f, \"totalMemory\": %d, " +
            "\"freeMemory\": %d, \"os\": \"%s\", \"memoryUsage\": %.2f, \"ipAddress\": \"%s\" }",
            hostname, cpu, totalMem, freeMem, osName, memUsage, ipAddress
        );

        // JSON 생성 후에도 BOM 방어 (혹시 모를 상황 대비)
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }

        return json;
    }
}
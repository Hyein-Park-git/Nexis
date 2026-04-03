package com.example.nexis.agent.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.io.File;

// agent.properties 파일을 읽어 설정값을 담는 클래스
public class AgentConfig {

    private final String host;
    private final int    port;
    private final int    interval;
    private final String agentName;
    private final String logDir;

    // 생성자에서 파일을 읽고 파싱 — 실패 시 IOException 발생
    public AgentConfig(String path) throws IOException {
        Properties prop = new Properties();

        // InputStreamReader with UTF-8: 한글 등 비ASCII 문자가 포함된 설정 파일도 안전하게 읽기
        try (InputStreamReader isr = new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8)) {
            prop.load(isr);
        }

        this.host     = getOrDefault(prop.getProperty("server.host"), "127.0.0.1");
        this.port     = Integer.parseInt(getOrDefault(prop.getProperty("server.port"), "9000"));
        this.interval = Integer.parseInt(getOrDefault(prop.getProperty("interval"), "30"));

        // Properties 파일에서 백슬래시(\)는 이스케이프 문자로 처리됨
        // "/"를 OS 구분자로 변환해 Windows/Linux 모두 정상 동작하게
        this.logDir = getOrDefault(prop.getProperty("log.dir"), "logs")
                          .replace("/", File.separator);

        // agent.name이 있으면 사용, 없으면 현재 호스트명을 자동으로 사용
        String nameProp = prop.getProperty("agent.name");
        if (nameProp != null && !nameProp.isBlank()) {
            this.agentName = nameProp.trim();
        } else {
            this.agentName = getLocalHostName();
        }
    }

    // null이거나 비어있으면 기본값 반환
    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }

    public String getHost()      { return host; }
    public int    getPort()      { return port; }
    public int    getInterval()  { return interval; }
    public String getAgentName() { return agentName; }
    public String getLogDir()    { return logDir; }

    // 호스트명 조회 — 실패 시 "UNKNOWN-PC" 반환
    private String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "UNKNOWN-PC";
        }
    }
}
package com.example.nexis.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// server.properties 파일을 읽어 서버 설정값을 담는 클래스
// 설정 파일 읽기 실패 시 RuntimeException 발생 → 서버 시작 자체를 중단
public class ServerConfig {

    private final String dbHost;
    private final int    dbPort;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private final int    tcpPort;
    private final String logDir;

    public ServerConfig() {
        Properties prop = new Properties();
        try (InputStreamReader isr = new InputStreamReader(
                new FileInputStream(new File("server.properties")), StandardCharsets.UTF_8)) {
            prop.load(isr);
        } catch (Exception e) {
            // 설정 파일 없으면 서버 시작 불가 → 즉시 종료
            throw new RuntimeException("Cannot load server.properties", e);
        }

        this.dbHost     = prop.getProperty("db.host",     "localhost");
        this.dbPort     = Integer.parseInt(prop.getProperty("db.port", "3306"));
        this.dbName     = prop.getProperty("db.name",     "nexis");
        this.dbUsername = prop.getProperty("db.username", "root");
        this.dbPassword = prop.getProperty("db.password", "");
        this.tcpPort    = Integer.parseInt(prop.getProperty("server.port", "9000"));
        this.logDir     = prop.getProperty("log.dir",     "logs");
    }

    public String getDbHost()     { return dbHost; }
    public int    getDbPort()     { return dbPort; }
    public String getDbName()     { return dbName; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public int    getTcpPort()    { return tcpPort; }
    public String getLogDir()     { return logDir; }

    // JDBC URL 조합 — MySQL 기준, SSL 비활성화 + 서울 타임존 고정
    public String getJdbcUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName
                + "?useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true";
    }
}
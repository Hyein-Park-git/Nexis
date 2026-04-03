package com.example.nexis.server;

import com.example.nexis.server.collector.TcpCollectorStarter;
import java.util.logging.Logger;

// 서버 진입점 — 설정 로딩, 로거 초기화, TCP Collector 시작, 트레이 아이콘(Windows) 등록
public class ServerApplication {

    public static void main(String[] args) {

        // server.properties 읽기
        ServerConfig config = new ServerConfig();

        // 로거 초기화 — 실패 시 콘솔 로거로 폴백
        final Logger logger;
        Logger tmp;
        try {
            tmp = ServerLoggerFactory.createLogger(config.getLogDir());
        } catch (Exception e) {
            e.printStackTrace();
            tmp = Logger.getLogger("ConsoleLogger"); // 파일 로거 실패 시 콘솔로 대체
        }
        logger = tmp;
        logger.info("=== Nexis Server starting ===");

        // TCP Collector 시작 — 에이전트로부터 메트릭 수신 대기
        TcpCollectorStarter starter = new TcpCollectorStarter(config, logger);
        starter.start();
        logger.info("TCP Collector started on port: " + config.getTcpPort());

        // Windows에서만 트레이 아이콘 등록
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            SystemTrayHelper.init(() -> {
                starter.stop();
                logger.info("TCP Collector stopped via tray exit");
                System.exit(0);
            });
            logger.info("System tray initialized");
        }

        // JVM 종료 시(Ctrl+C, kill 등) TCP Collector 정리
        // ShutdownHook: 프로세스 종료 직전에 자동 실행되는 스레드
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            starter.stop();
            logger.info("TCP Collector stopped");
        }));
    }
}
package com.example.nexis.server.collector;

import com.example.nexis.server.ServerConfig;
import java.util.logging.Logger;

// TcpCollector를 별도 스레드로 시작/정지하는 래퍼 클래스
// ServerApplication이 직접 Thread를 다루지 않도록 캡슐화
public class TcpCollectorStarter {

    private TcpCollector collector;
    private Thread       collectorThread;
    private final Logger logger;

    public TcpCollectorStarter(ServerConfig config, Logger logger) {
        this.logger  = logger;
        TcpCollectorDao dao = new TcpCollectorDao(config, logger);
        collector = new TcpCollector(config.getTcpPort(), dao, logger);
    }

    // TcpCollector를 새 스레드에서 실행
    public void start() {
        if (logger != null)
            logger.info("[Starter] Starting TCP Collector");
        collectorThread = new Thread(collector, "TcpCollectorThread");
        collectorThread.start();
    }

    // 수신 루프 종료 + 스레드 인터럽트
    public void stop() {
        if (logger != null)
            logger.info("[Starter] Stopping TCP Collector");
        if (collector != null)     collector.stop();
        // 스레드가 아직 살아있으면 인터럽트로 블로킹(accept) 해제
        if (collectorThread != null && collectorThread.isAlive())
            collectorThread.interrupt();
    }
}
package com.example.nexis.server.collector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

// TCP 서버 역할 — 에이전트로부터 JSON 메트릭을 수신해 DB에 저장
// Runnable 구현 → Thread에서 실행 가능
public class TcpCollector implements Runnable {

    private final int            port;
    private final TcpCollectorDao dao;
    private final Logger         logger;

    // volatile: 다른 스레드에서 stop()을 호출했을 때 즉시 반영되도록 보장
    private volatile boolean     running = true;

    // 동시 접속 처리를 위한 스레드 풀 (최대 10개 동시 처리)
    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public TcpCollector(int port, TcpCollectorDao dao, Logger logger) {
        this.port   = port;
        this.dao    = dao;
        this.logger = logger;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("[TCP] Listening on port: " + port);

            // running이 false가 될 때까지 새 연결을 계속 수락
            while (running) {
                Socket socket = serverSocket.accept(); // 새 에이전트 연결 대기 (블로킹)
                pool.submit(() -> handle(socket));     // 연결마다 별도 스레드로 처리
            }
        } catch (Exception e) {
            logger.severe("[TCP] Server error: " + e.getMessage());
        }
    }

    // 에이전트 소켓에서 JSON을 라인 단위로 읽어 DB 저장
    private void handle(Socket socket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;

                // 라인에서 JSON 시작 위치({) 찾기 — BOM이나 앞쪽 쓰레기 값 제거
                int jsonStart = line.indexOf('{');
                if (jsonStart == -1) {
                    logger.warning("[TCP] No JSON object found in line: " + line);
                    continue;
                }

                String jsonPart = line.substring(jsonStart).trim();
                logger.info("[TCP] Received raw (parsed): " + jsonPart);

                dao.saveMetric(jsonPart); // DB 저장 위임
            }
        } catch (Exception e) {
            logger.severe("[TCP] Socket handler error: " + e.getMessage());
        }
    }

    // 외부에서 수신 루프를 종료하고 스레드 풀을 정리
    public void stop() {
        running = false;
        pool.shutdown(); // 기존 작업은 마무리하고 새 작업은 안 받음
    }
}
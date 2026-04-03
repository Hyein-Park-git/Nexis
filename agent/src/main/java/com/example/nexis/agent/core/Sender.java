package com.example.nexis.agent.core;

import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

// 수집한 JSON 데이터를 TCP 소켓으로 서버에 전송하는 클래스
public class Sender {

    private final AgentConfig config;
    private static final Logger logger = Logger.getLogger("NexisAgentLogger");

    public Sender(AgentConfig config) {
        this.config = config;
    }

    public void send(String json) {
        try {
            // BOM(Byte Order Mark) 제거 — 일부 Windows 환경에서 UTF-8 파일 앞에 \uFEFF가 붙는 경우 방어
            if (json.startsWith("\uFEFF")) {
                json = json.substring(1);
            }

            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

            // try-with-resources: 전송 후 소켓/스트림 자동 close
            try (Socket socket = new Socket(config.getHost(), config.getPort());
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

                // 프로토콜: [4바이트 길이][JSON 바이트]
                // 서버가 먼저 데이터 크기를 읽고, 그만큼만 수신하도록 길이를 먼저 전송
                dos.writeInt(jsonBytes.length);
                dos.write(jsonBytes);
                dos.flush();

                logger.info("[Sender] Data sent successfully to "
                        + config.getHost() + ":" + config.getPort()
                        + " (" + jsonBytes.length + " bytes)");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Sender] Failed to send data via TCP to "
                    + config.getHost() + ":" + config.getPort(), e);
        }
    }
}
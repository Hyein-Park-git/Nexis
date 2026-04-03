package com.example.nexis.agent.core;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

// 파일 로거를 생성하는 팩토리 클래스
// 쓰기 가능한 디렉토리를 우선순위대로 탐색해 로그 파일을 생성
public class LoggerFactory {

    public static Logger createLogger(String logDir) throws IOException {
        File dir = resolveWritableDir(logDir);
        String logFilePath = dir.getAbsolutePath() + File.separator + "nexis-agent.log";
        System.out.println("[Logger] Log file path: " + logFilePath);

        Logger logger = Logger.getLogger("NexisAgentLogger");
        logger.setUseParentHandlers(false); // 루트 로거(콘솔)로 전파 차단

        // 핸들러가 없을 때만 추가 (중복 등록 방지)
        if (logger.getHandlers().length == 0) {
            // 날짜/시간 포맷을 영문 고정 (로케일 영향 없이 일관된 형식 유지)
            System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n"
            );

            // FileHandler(path, append=true): 앱 재시작 시 기존 로그를 이어서 기록
            FileHandler handler = new FileHandler(logFilePath, true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
            logger.setLevel(Level.INFO);
        }

        return logger;
    }

    // 쓰기 가능한 로그 디렉토리를 우선순위대로 탐색
    private static File resolveWritableDir(String logDir) {
        // 1순위: agent.properties의 log.dir
        // 상대 경로면 user.dir 기준으로 절대 경로로 변환
        File dir = new File(logDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), logDir);
        }
        System.out.println("[Logger] Trying log.dir: " + dir.getAbsolutePath());
        if (isWritable(dir)) return dir;

        // 2순위: C:\ProgramData\NexisAgent\logs (Windows 설치 환경)
        String programData = System.getenv("PROGRAMDATA");
        if (programData != null) {
            File fallback1 = new File(programData, "NexisAgent" + File.separator + "logs");
            System.out.println("[Logger] Trying ProgramData fallback: " + fallback1.getAbsolutePath());
            if (isWritable(fallback1)) return fallback1;
        }

        // 3순위: /var/log/nexis-agent (Linux 서비스 환경)
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            File fallback2 = new File("/var/log/nexis-agent");
            System.out.println("[Logger] Trying /var/log fallback: " + fallback2.getAbsolutePath());
            if (isWritable(fallback2)) return fallback2;
        }

        // 4순위: 홈 디렉토리 (최후의 수단 — 항상 접근 가능)
        File fallback3 = new File(System.getProperty("user.home"),
                "NexisAgent" + File.separator + "logs");
        System.out.println("[Logger] Trying user.home fallback: " + fallback3.getAbsolutePath());
        fallback3.mkdirs();
        return fallback3;
    }

    // 해당 디렉토리에 실제로 파일을 쓸 수 있는지 테스트
    private static boolean isWritable(File dir) {
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                System.out.println("[Logger] Failed to create directory: " + dir.getAbsolutePath());
                return false;
            }
            // 임시 파일 생성/삭제로 쓰기 권한 확인
            File test = new File(dir, ".write_test");
            if (test.createNewFile()) {
                test.delete();
                return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("[Logger] Not writable: " + dir.getAbsolutePath() + " - " + e.getMessage());
            return false;
        }
    }
}
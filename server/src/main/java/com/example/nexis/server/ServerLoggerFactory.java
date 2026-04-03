package com.example.nexis.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

// 서버용 파일 로거 생성 팩토리
// 에이전트의 LoggerFactory와 동일한 구조지만 폴백 경로가 더 단순함
public class ServerLoggerFactory {

    public static Logger createLogger(String logDir) throws IOException {
        File dir = resolveWritableDir(logDir);
        String logFilePath = dir.getAbsolutePath() + File.separator + "nexis-server.log";

        Logger logger = Logger.getLogger("NexisServerLogger");
        logger.setUseParentHandlers(false); // 루트 로거(콘솔)로 전파 차단

        if (logger.getHandlers().length == 0) {
            // 날짜 포맷 지정 — 로케일 무관하게 영문으로 출력
            System.setProperty(
                "java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$s %5$s%6$s%n"
            );
            FileHandler handler = new FileHandler(logFilePath, true); // append 모드
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
            logger.setLevel(Level.INFO);
        }

        return logger;
    }

    // 쓰기 가능한 로그 디렉토리 탐색
    // 1순위: server.properties의 log.dir
    // 2순위: user.home/NexisServer/logs (최후 폴백)
    private static File resolveWritableDir(String logDir) {
        File dir = new File(logDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), logDir);
        }
        if (isWritable(dir)) return dir;

        File fallback = new File(System.getProperty("user.home"),
                "NexisServer" + File.separator + "logs");
        fallback.mkdirs();
        return fallback;
    }

    // 임시 파일 생성으로 쓰기 권한 확인
    private static boolean isWritable(File dir) {
        try {
            if (!dir.exists() && !dir.mkdirs()) return false;
            File test = new File(dir, ".write_test");
            if (test.createNewFile()) test.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
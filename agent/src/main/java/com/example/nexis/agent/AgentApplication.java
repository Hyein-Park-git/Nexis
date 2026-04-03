package com.example.nexis.agent;

import com.example.nexis.agent.core.AgentConfig;
import com.example.nexis.agent.core.LoggerFactory;
import com.example.nexis.agent.core.Sender;
import com.example.nexis.agent.core.SystemInfoCollector;

import java.awt.*;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

// 에이전트 진입점 — 설정 로딩, 트레이 초기화(Windows), 수집 스케줄러 시작
public class AgentApplication {

    private static Logger logger = Logger.getLogger("NexisAgentLogger");

    public static void main(String[] args) {
        // 트레이 아이콘을 사용하려면 headless 모드를 꺼야 함
        // headless=true면 AWT/Swing 관련 기능이 모두 비활성화됨
        System.setProperty("java.awt.headless", "false");

        try {
            String installDir = resolveInstallDir();
            String configPath = installDir + File.separator + "agent.properties";
            String iconPath   = installDir + File.separator + "icon.png";

            AgentConfig config = new AgentConfig(configPath);

            // 설정 파일에서 읽은 log.dir로 파일 로거 초기화
            logger = LoggerFactory.createLogger(config.getLogDir());

            logger.info("==== NEXIS Agent Starting ====");
            logger.info("[Startup] OS          : " + System.getProperty("os.name"));
            logger.info("[Startup] Install dir : " + installDir);
            logger.info("[Startup] Config path : " + configPath);
            logger.info("[Startup] Log dir     : " + config.getLogDir());
            logger.info("[Startup] Server      : " + config.getHost() + ":" + config.getPort());
            logger.info("[Startup] Interval    : " + config.getInterval() + "s");
            logger.info("[Startup] Agent name  : " + config.getAgentName());

            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");

            if (isWindows) {
                initTray(iconPath); // Windows에서만 시스템 트레이 아이콘 초기화
            } else {
                logger.info("[Startup] Non-Windows OS - running in headless service mode");
            }

            startScheduler(config); // OS 공통: 메트릭 수집 + 전송 스케줄러 시작

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Startup] Failed to start agent", e);
            e.printStackTrace();
        }
    }

    // Windows 전용 트레이 아이콘 초기화
    private static void initTray(String iconPath) {
        if (GraphicsEnvironment.isHeadless()) {
            logger.severe("[Tray] Headless environment - tray icon unavailable");
            return;
        }

        if (!SystemTray.isSupported()) {
            logger.severe("[Tray] SystemTray not supported on this platform");
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            // 아이콘 로딩 실패해도 트레이는 계속 등록 시도
            Image image = null;
            try {
                image = javax.imageio.ImageIO.read(new File(iconPath));
                logger.info("[Tray] Icon loaded: " + iconPath);
            } catch (Exception e) {
                logger.warning("[Tray] Failed to load icon: " + iconPath);
            }

            // 우클릭 팝업 — Exit 항목만 제공
            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                logger.info("[Tray] Exit clicked - shutting down agent");
                System.exit(0);
            });
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "NEXIS Agent", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            logger.info("[Tray] Tray icon registered successfully");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[Tray] Failed to initialize tray icon", e);
        }
    }

    // 메트릭 수집 + 전송 스케줄러 시작
    private static void startScheduler(AgentConfig config) {
        // newScheduledThreadPool(2): 스레드 2개짜리 풀 (수집용 + 상태 로그용)
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

        // 메트릭 수집 및 서버 전송 — 0초 딜레이 후 interval 초마다 반복
        // scheduleAtFixedRate: 이전 실행 시작 기준으로 interval마다 실행 (지연과 무관)
        executor.scheduleAtFixedRate(() -> {
            try {
                String json = SystemInfoCollector.collect(config.getAgentName());
                logger.info("[Collector] Sending raw data:\n" + json);
                new Sender(config).send(json);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[Collector] Failed to collect or send data", e);
            }
        }, 0, config.getInterval(), TimeUnit.SECONDS);

        // 30초마다 에이전트가 살아있음을 로그로 알림 (heartbeat 로그)
        executor.scheduleAtFixedRate(
            () -> logger.info("[Agent] Running... (interval: " + config.getInterval() + "s)"),
            30, 30, TimeUnit.SECONDS
        );

        logger.info("[Scheduler] Started - collecting every " + config.getInterval() + "s");
    }

    // 에이전트 설치 경로 결정
    // 1순위: JAR/EXE 파일의 실제 위치 (배포 환경)
    // 2순위: user.dir (IDE 실행 환경)
    private static String resolveInstallDir() {
        try {
            File exeFile = new File(
                AgentApplication.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
            );
            return exeFile.getParent();
        } catch (Exception e) {
            System.err.println("[Startup] Failed to resolve install dir - using user.dir");
            return System.getProperty("user.dir");
        }
    }
}
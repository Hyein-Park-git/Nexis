package com.example.nexis.server;

import java.awt.*;

// Windows 전용 시스템 트레이 아이콘 헬퍼
// Runnable onExit를 주입받아 종료 동작을 외부에서 결정할 수 있게 함 (콜백 패턴)
public class SystemTrayHelper {

    public static void init(Runnable onExit) {
        if (!SystemTray.isSupported()) return; // 트레이 미지원 환경이면 조용히 스킵

        try {
            SystemTray tray  = SystemTray.getSystemTray();
            Image      image = Toolkit.getDefaultToolkit().createImage("icon.png");

            // 우클릭 팝업 — Exit 항목만 제공
            PopupMenu popup    = new PopupMenu();
            MenuItem  exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> onExit.run()); // 외부에서 주입한 종료 로직 실행
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Nexis Server", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);

        } catch (Exception e) {
            e.printStackTrace(); // 트레이 실패해도 서버는 계속 실행
        }
    }
}
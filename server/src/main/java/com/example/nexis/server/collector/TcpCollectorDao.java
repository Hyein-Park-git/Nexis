package com.example.nexis.server.collector;

import com.example.nexis.server.ServerConfig;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

// 수신한 JSON 메트릭을 DB에 저장하는 DAO(Data Access Object)
// 호스트 자동 등록 + 아이템별 수집값 저장을 담당
public class TcpCollectorDao {

    private final ServerConfig config;
    private final Logger       logger;

    public TcpCollectorDao(ServerConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    // JSON 메트릭 파싱 → 호스트 등록/업데이트 → 아이템별 값 저장
    public void saveMetric(String jsonValue) {
        try {
            JSONObject json    = new JSONObject(jsonValue);
            String hostname    = json.optString("hostname");
            String ipAddress   = json.optString("ipAddress");
            String os          = json.optString("os");
            double cpuUsage    = json.optDouble("cpuUsage",    0);
            long   totalMemory = json.optLong("totalMemory",   0);
            long   freeMemory  = json.optLong("freeMemory",    0);
            double memUsage    = json.optDouble("memoryUsage", 0);

            if (hostname == null || hostname.isBlank()) {
                logger.warning("[DB] Skipping metric: hostname is blank");
                return;
            }

            // 1단계: 호스트가 없으면 자동 등록, 있으면 상태 업데이트
            long hostId = ensureHostExists(hostname, ipAddress, os);
            if (hostId < 0) {
                logger.warning("[DB] Could not get hostId for: " + hostname);
                return;
            }

            // 2단계: 해당 호스트에 등록된 활성 아이템 목록 조회
            List<Item> items = findItemsByHostId(hostId);
            if (items.isEmpty()) {
                logger.info("[DB] No items for host: " + hostname + ", skipping item_data insert");
                return;
            }

            // 3단계: 각 아이템의 itemKey에 맞는 값을 추출해 item_data 저장
            for (Item item : items) {
                Double value = extractValue(item.itemKey, item.unitDisplay,
                        cpuUsage, totalMemory, freeMemory, memUsage);
                if (value != null) {
                    insertItemData(item.id, hostname, value, item.interval); // interval 전달
                }
            }

            logger.info("[DB] Saved item_data for host=" + hostname +
                    " cpu=" + cpuUsage + "% mem=" + memUsage + "%");

        } catch (Exception e) {
            logger.severe("[DB] Failed to save metric: " + e.getMessage());
        }
    }

    // 호스트가 이미 있으면 상태 업데이트(active + lastCheck), 없으면 신규 등록
    private long ensureHostExists(String hostname, String ipAddress, String os) {
        String selectSql = "SELECT id FROM hosts WHERE hostname = ?";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, hostname);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // 기존 호스트 — IP/OS 업데이트 (빈값이면 기존값 유지: COALESCE + NULLIF 조합)
                long hostId = rs.getLong("id");
                String updateSql = "UPDATE hosts SET " +
                                   "ip_address = COALESCE(NULLIF(?, ''), ip_address), " +
                                   "os = COALESCE(NULLIF(?, ''), os), " +
                                   "agent_active = 1, " +
                                   "agent_last_check = NOW() " +
                                   "WHERE hostname = ?";
                try (Connection conn2 = getConn();
                     PreparedStatement ps2 = conn2.prepareStatement(updateSql)) {
                    ps2.setString(1, ipAddress);
                    ps2.setString(2, os);
                    ps2.setString(3, hostname);
                    ps2.executeUpdate();
                }
                return hostId;

            } else {
                // 신규 호스트 자동 등록
                String insertSql = "INSERT INTO hosts " +
                                   "(hostname, ip_address, os, enabled, agent_active, agent_last_check) " +
                                   "VALUES (?, ?, ?, 1, 1, NOW())";
                try (Connection conn2 = getConn();
                     PreparedStatement ps2 = conn2.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps2.setString(1, hostname);
                    ps2.setString(2, ipAddress != null && !ipAddress.isBlank() ? ipAddress : null);
                    ps2.setString(3, os        != null && !os.isBlank()        ? os        : null);
                    ps2.executeUpdate();

                    // RETURN_GENERATED_KEYS: INSERT 후 자동 생성된 PK 값을 가져옴
                    ResultSet keys = ps2.getGeneratedKeys();
                    if (keys.next()) {
                        long hostId = keys.getLong(1);
                        logger.info("[DB] Auto-registered host: " + hostname + " id=" + hostId);
                        addToDefaultGroup(hostId); // 신규 호스트는 Default Group에 자동 등록
                        return hostId;
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("[DB] ensureHostExists failed: " + e.getMessage());
        }
        return -1;
    }

    // 신규 호스트를 Default Group에 자동으로 추가
    private void addToDefaultGroup(long hostId) {
        try (Connection conn = getConn()) {
            // Default Group ID 조회
            String selectGroupSql = "SELECT id FROM host_groups WHERE name = 'Default Group' LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(selectGroupSql)) {
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) return; // Default Group 없으면 스킵

                long groupId = rs.getLong("id");

                // 이미 등록된 경우 중복 방지
                String checkSql = "SELECT COUNT(*) FROM host_group_members WHERE group_id = ? AND host_id = ?";
                try (PreparedStatement ps2 = conn.prepareStatement(checkSql)) {
                    ps2.setLong(1, groupId);
                    ps2.setLong(2, hostId);
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next() && rs2.getLong(1) > 0) return;
                }

                // Default Group에 등록
                String insertSql = "INSERT INTO host_group_members (group_id, host_id) VALUES (?, ?)";
                try (PreparedStatement ps3 = conn.prepareStatement(insertSql)) {
                    ps3.setLong(1, groupId);
                    ps3.setLong(2, hostId);
                    ps3.executeUpdate();
                    logger.info("[DB] Added host " + hostId + " to Default Group");
                }
            }
        } catch (Exception e) {
            logger.warning("[DB] addToDefaultGroup failed: " + e.getMessage());
        }
    }

    // 호스트의 활성화된 아이템 목록 조회 (itemKey, unitDisplay, interval 포함)
    private List<Item> findItemsByHostId(long hostId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT id, item_key, unit_display, `interval` FROM items WHERE host_id = ? AND enabled = 1";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, hostId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Item item        = new Item();
                item.id          = rs.getLong("id");
                item.itemKey     = rs.getString("item_key");
                item.unitDisplay = rs.getString("unit_display");
                item.interval    = rs.getInt("interval"); // 수집 주기 (초)
                items.add(item);
            }
        } catch (Exception e) {
            logger.warning("[DB] findItemsByHostId failed: " + e.getMessage());
        }
        return items;
    }

    // item_data 테이블에 수집값 저장
    // interval 체크: 마지막 수집 시각으로부터 interval초가 지나지 않았으면 저장 스킵
    private void insertItemData(long itemId, String hostname, Double value, int interval) {
        // 마지막 수집 시각 조회
        String lastSql = "SELECT collected_at FROM item_data WHERE item_id = ? ORDER BY collected_at DESC LIMIT 1";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(lastSql)) {
            ps.setLong(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Timestamp last = rs.getTimestamp("collected_at");
                if (last != null) {
                    long elapsedSeconds = (System.currentTimeMillis() - last.getTime()) / 1000;
                    if (elapsedSeconds < (long)(interval * 0.7)) {
                        // interval의 70%가 아직 안 됐으면 저장 스킵 (네트워크 지연/처리 시간 오차 감안)
                        logger.fine("[DB] Skipping itemId=" + itemId
                                + " elapsed=" + elapsedSeconds + "s < interval*0.7=" + (long)(interval * 0.7) + "s");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("[DB] interval check failed for itemId=" + itemId + ": " + e.getMessage());
        }

        // interval 지났으면 저장
        String sql = "INSERT INTO item_data (item_id, hostname, value) VALUES (?, ?, ?)";
        try (Connection conn = getConn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, itemId);
            ps.setString(2, hostname);
            ps.setDouble(3, value);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warning("[DB] insertItemData failed: " + e.getMessage());
        }
    }

    // itemKey와 unitDisplay를 보고 JSON에서 적절한 값을 추출 + 단위 변환
    // 예: "memory.available" + unitDisplay="GB" → freeMemory bytes를 GB로 변환
    private Double extractValue(String itemKey, String unitDisplay,
                                double cpuUsage, long totalMemory,
                                long freeMemory, double memUsage) {
        if (itemKey == null) return null;

        // itemKey별 원시값 추출
        Double raw = switch (itemKey) {
            case "cpu.usage"        -> cpuUsage;
            case "cpu.available"    -> 100.0 - cpuUsage;
            case "memory.usage"     -> memUsage;
            case "memory.available" -> (double) freeMemory;
            case "memory.total"     -> (double) totalMemory;
            case "memory.free"      -> (double) freeMemory;
            default -> null; // 알 수 없는 itemKey는 저장 안 함
        };
        if (raw == null) return null;

        // unitDisplay에 따라 단위 변환 (메모리 관련 값은 bytes → 지정 단위)
        String unit = unitDisplay != null ? unitDisplay : "";
        return switch (unit) {
            case "GB"    -> raw / 1073741824.0; // 1024^3
            case "MB"    -> raw / 1048576.0;    // 1024^2
            case "KB"    -> raw / 1024.0;
            case "Bytes" -> raw;
            default      -> raw;                // "%", "" 등 변환 불필요
        };
    }

    // 매번 새 커넥션 생성 (커넥션 풀 미사용 — 트래픽이 낮은 환경 대상)
    private Connection getConn() throws SQLException {
        return DriverManager.getConnection(
                config.getJdbcUrl(),
                config.getDbUsername(),
                config.getDbPassword());
    }

    // DB 조회 결과를 담는 내부 DTO
    private static class Item {
        long   id;
        String itemKey;
        String unitDisplay;
        int    interval = 60; // 기본값 60초 (DB에 저장된 값으로 덮어씀)
    }
}
package cn.iocoder.yudao.server.service.app;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class AppClientCommandService {

    public static final String COMMAND_CLEAR_USER_CACHE = "CLEAR_USER_CACHE";
    private static final int CLEAR_COMMAND_EXPIRE_DAYS = 30;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    public HeartbeatRespVO heartbeat(HeartbeatReqVO reqVO) {
        String deviceUuid = requireDeviceUuid(reqVO == null ? null : reqVO.getDeviceUuid());
        Long userId = reqVO == null ? null : reqVO.getUserId();
        if (userId != null && isActiveUser(userId)) {
            upsertDevice(userId, deviceUuid, reqVO);
        }

        List<ClientCommandRespVO> commands = jdbcTemplate.query("""
                SELECT id, command_type, status, payload_json, delivered_at, expires_at
                FROM app_client_command
                WHERE device_uuid = ? AND command_type = ? AND status = 'pending'
                  AND deleted = 0 AND expires_at > NOW()
                ORDER BY create_time ASC, id ASC
                LIMIT 20
                """, (rs, rowNum) -> {
            ClientCommandRespVO command = new ClientCommandRespVO();
            command.setId(rs.getLong("id"));
            command.setType(rs.getString("command_type"));
            command.setStatus(rs.getString("status"));
            command.setPayloadJson(StrUtil.nullToDefault(rs.getString("payload_json"), "{}"));
            command.setDeliveredAt(rs.getTimestamp("delivered_at") == null
                    ? null : rs.getTimestamp("delivered_at").toLocalDateTime());
            command.setExpiresAt(rs.getTimestamp("expires_at") == null
                    ? null : rs.getTimestamp("expires_at").toLocalDateTime());
            return command;
        }, deviceUuid, COMMAND_CLEAR_USER_CACHE);

        if (!commands.isEmpty()) {
            jdbcTemplate.update("""
                    UPDATE app_client_command
                    SET delivered_at = IFNULL(delivered_at, NOW()), updater = 'app', update_time = NOW()
                    WHERE device_uuid = ? AND command_type = ? AND status = 'pending'
                      AND deleted = 0 AND expires_at > NOW()
                    """, deviceUuid, COMMAND_CLEAR_USER_CACHE);
        }

        HeartbeatRespVO respVO = new HeartbeatRespVO();
        respVO.setCommands(commands);
        return respVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean ack(Long commandId, CommandAckReqVO reqVO) {
        if (commandId == null) {
            throw ServiceExceptionUtil.invalidParamException("清理指令不存在，请稍后重试");
        }
        String deviceUuid = requireDeviceUuid(reqVO == null ? null : reqVO.getDeviceUuid());
        String status = reqVO == null ? "" : StrUtil.blankToDefault(reqVO.getStatus(), "acknowledged");
        String normalizedStatus = "failed".equalsIgnoreCase(status) ? "failed" : "acknowledged";
        int updated = jdbcTemplate.update("""
                UPDATE app_client_command
                SET status = ?, acknowledged_at = NOW(), error_message = ?, updater = 'app', update_time = NOW()
                WHERE id = ? AND device_uuid = ? AND command_type = ? AND status = 'pending' AND deleted = 0
                """, normalizedStatus,
                StrUtil.sub(StrUtil.blankToDefault(reqVO == null ? null : reqVO.getErrorMessage(), ""), 0, 512),
                commandId, deviceUuid, COMMAND_CLEAR_USER_CACHE);
        if (updated == 0) {
            throw ServiceExceptionUtil.invalidParamException("清理指令已处理");
        }
        return true;
    }

    public void createClearUserCacheCommands(Long userId, List<String> deviceUuids) {
        if (userId == null || deviceUuids == null || deviceUuids.isEmpty()) {
            return;
        }
        if (!hasColumns("app_client_command", "user_id", "device_uuid", "command_type", "status",
                "payload_json", "expires_at", "deleted", "creator", "updater", "update_time")) {
            return;
        }
        LinkedHashSet<String> uniqueDeviceUuids = new LinkedHashSet<>();
        for (String deviceUuid : deviceUuids) {
            if (StrUtil.isNotBlank(deviceUuid)) {
                uniqueDeviceUuids.add(deviceUuid.trim());
            }
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(CLEAR_COMMAND_EXPIRE_DAYS);
        for (String deviceUuid : uniqueDeviceUuids) {
            Integer existing = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM app_client_command
                    WHERE user_id = ? AND device_uuid = ? AND command_type = ? AND status = 'pending' AND deleted = 0
                    """, Integer.class, userId, deviceUuid, COMMAND_CLEAR_USER_CACHE);
            if (existing != null && existing > 0) {
                jdbcTemplate.update("""
                        UPDATE app_client_command
                        SET expires_at = ?, updater = 'admin', update_time = NOW()
                        WHERE user_id = ? AND device_uuid = ? AND command_type = ? AND status = 'pending' AND deleted = 0
                        """, expiresAt, userId, deviceUuid, COMMAND_CLEAR_USER_CACHE);
                continue;
            }
            jdbcTemplate.update("""
                    INSERT INTO app_client_command(user_id, device_uuid, command_type, status, payload_json,
                                                   expires_at, creator, updater)
                    VALUES (?, ?, ?, 'pending', '{}', ?, 'admin', 'admin')
                    """, userId, deviceUuid, COMMAND_CLEAR_USER_CACHE, expiresAt);
        }
    }

    private void upsertDevice(Long userId, String deviceUuid, HeartbeatReqVO reqVO) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM app_device WHERE device_uuid = ? AND deleted = 0
                """, Integer.class, deviceUuid);
        if (count != null && count > 0) {
            jdbcTemplate.update("""
                    UPDATE app_device
                    SET user_id = ?, platform = 'android', app_version = ?, device_model = ?, os_version = ?,
                        network_policy = ?, last_seen_at = NOW(), updater = 'app', update_time = NOW()
                    WHERE device_uuid = ? AND deleted = 0
                    """, userId,
                    StrUtil.blankToDefault(reqVO.getAppVersion(), ""),
                    StrUtil.blankToDefault(reqVO.getDeviceModel(), ""),
                    StrUtil.blankToDefault(reqVO.getOsVersion(), ""),
                    StrUtil.blankToDefault(reqVO.getNetworkPolicy(), "online_first"),
                    deviceUuid);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO app_device(user_id, device_uuid, platform, app_version, device_model,
                                       os_version, network_policy, last_seen_at, creator, updater)
                VALUES (?, ?, 'android', ?, ?, ?, ?, NOW(), 'app', 'app')
                """, userId, deviceUuid,
                StrUtil.blankToDefault(reqVO.getAppVersion(), ""),
                StrUtil.blankToDefault(reqVO.getDeviceModel(), ""),
                StrUtil.blankToDefault(reqVO.getOsVersion(), ""),
                StrUtil.blankToDefault(reqVO.getNetworkPolicy(), "online_first"));
    }

    private boolean isActiveUser(Long userId) {
        try {
            Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM app_user WHERE id = ? AND deleted = 0 AND status = 1
                    """, Integer.class, userId);
            return count != null && count > 0;
        } catch (EmptyResultDataAccessException ex) {
            return false;
        }
    }

    private boolean hasColumns(String table, String... columns) {
        if (!tableExists(table)) {
            return false;
        }
        for (String column : columns) {
            if (!columnExists(table, column)) {
                return false;
            }
        }
        return true;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = ?
                """, Integer.class, table);
        return count != null && count > 0;
    }

    private String requireDeviceUuid(String deviceUuid) {
        if (StrUtil.isBlank(deviceUuid)) {
            throw ServiceExceptionUtil.invalidParamException("设备信息缺失，请重新打开 App 后再试");
        }
        return deviceUuid.trim();
    }

    @Data
    public static class HeartbeatReqVO {
        private Long userId;
        private String deviceUuid;
        private String appVersion;
        private String deviceModel;
        private String osVersion;
        private String networkPolicy;
    }

    @Data
    public static class HeartbeatRespVO {
        private List<ClientCommandRespVO> commands = new ArrayList<>();
    }

    @Data
    public static class ClientCommandRespVO {
        private Long id;
        private String type;
        private String status;
        private String payloadJson;
        private LocalDateTime deliveredAt;
        private LocalDateTime expiresAt;
    }

    @Data
    public static class CommandAckReqVO {
        private String deviceUuid;
        private String status;
        private String errorMessage;
    }
}

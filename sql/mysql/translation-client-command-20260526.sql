USE `ruoyi-vue-pro`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_client_command` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Client command id',
  `user_id` BIGINT UNSIGNED NULL COMMENT 'App user id',
  `device_uuid` VARCHAR(128) NOT NULL DEFAULT '' COMMENT 'Device uuid',
  `command_type` VARCHAR(64) NOT NULL COMMENT 'Command type',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/acknowledged/failed',
  `payload_json` JSON NULL COMMENT 'Command payload',
  `delivered_at` DATETIME NULL COMMENT 'Delivered time',
  `acknowledged_at` DATETIME NULL COMMENT 'Acknowledged time',
  `expires_at` DATETIME NOT NULL COMMENT 'Expire time',
  `error_message` VARCHAR(512) NOT NULL DEFAULT '' COMMENT 'Client error message',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Creator',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Updater',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  KEY `idx_client_command_device` (`device_uuid`, `status`, `expires_at`),
  KEY `idx_client_command_user` (`user_id`, `status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App client command';

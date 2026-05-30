USE `ruoyi-vue-pro`;

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `app_integration_secret` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Config id',
  `config_key` VARCHAR(96) NOT NULL COMMENT 'Config key',
  `config_value` TEXT NULL COMMENT 'Encrypted config value',
  `encrypted` TINYINT NOT NULL DEFAULT 1 COMMENT '1 encrypted, 0 plain',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Remark',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Creator',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Updater',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_integration_secret_key` (`config_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Backend integration secret config';

CREATE TABLE IF NOT EXISTS `app_user_session` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Session id',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT 'App user id',
  `access_token` VARCHAR(96) NOT NULL COMMENT 'Access token',
  `refresh_token` VARCHAR(96) NOT NULL COMMENT 'Refresh token',
  `expires_at` DATETIME NOT NULL COMMENT 'Access token expire time',
  `refresh_expires_at` DATETIME NOT NULL COMMENT 'Refresh token expire time',
  `client_type` VARCHAR(16) NOT NULL DEFAULT 'web' COMMENT 'web/android',
  `last_seen_at` DATETIME NULL COMMENT 'Last seen time',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Creator',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Updater',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_user_session_access` (`access_token`, `deleted`),
  UNIQUE KEY `uk_app_user_session_refresh` (`refresh_token`, `deleted`),
  KEY `idx_app_user_session_user` (`user_id`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App user session';

CREATE TABLE IF NOT EXISTS `app_offline_membership_product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Product id',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU code',
  `name` VARCHAR(64) NOT NULL COMMENT 'Product name',
  `duration_days` INT NOT NULL DEFAULT 0 COMMENT 'Duration days, 0 means permanent',
  `price_cent` INT NOT NULL COMMENT 'Price in cents',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT 'Currency',
  `description` VARCHAR(255) NOT NULL DEFAULT '' COMMENT 'Description',
  `tag` VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'Display tag',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
  `sort` INT NOT NULL DEFAULT 0 COMMENT 'Sort',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Creator',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'Updater',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT 'Deleted flag',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Tenant id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offline_membership_product_sku` (`sku_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='App offline membership product';

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'pay_order_id'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `pay_order_id` BIGINT NULL COMMENT ''Pay module order id'' AFTER `provider_trade_no`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'pay_order_no'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `pay_order_no` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''Pay order no'' AFTER `pay_order_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'pay_channel_code'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `pay_channel_code` VARCHAR(32) NOT NULL DEFAULT ''alipay_wap'' COMMENT ''Pay channel code'' AFTER `pay_channel`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'client_type'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `client_type` VARCHAR(16) NOT NULL DEFAULT ''web'' COMMENT ''web/android'' AFTER `product_id`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'expire_time'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `expire_time` DATETIME NULL COMMENT ''Payment expire time'' AFTER `paid_at`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'closed_at'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `closed_at` DATETIME NULL COMMENT ''Closed time'' AFTER `expire_time`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'fail_reason'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `fail_reason` VARCHAR(255) NOT NULL DEFAULT '''' COMMENT ''Fail reason'' AFTER `closed_at`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'app_payment_order' AND column_name = 'alipay_trade_status'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE `app_payment_order` ADD COLUMN `alipay_trade_status` VARCHAR(32) NOT NULL DEFAULT '''' COMMENT ''Alipay trade status'' AFTER `fail_reason`',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(1) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'app_payment_order'
    AND index_name = 'idx_payment_status_expire'
);
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE `app_payment_order` ADD INDEX `idx_payment_status_expire` (`status`, `expire_time`)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `app_token_product` (`sku_code`, `name`, `token_amount`, `bonus_tokens`, `price_cent`, `currency`, `status`, `sort`, `creator`, `updater`)
VALUES
  ('TOKEN_SANDBOX_001', 'Alipay sandbox test package', 1, 0, 1, 'CNY', 1, 1, 'seed', 'seed'),
  ('ONLINE_LITE_001', 'Online lite package', 35000, 0, 1, 'CNY', 1, 11, 'seed', 'seed'),
  ('ONLINE_STANDARD_001', 'Online standard package', 80000, 0, 1900, 'CNY', 1, 12, 'seed', 'seed'),
  ('ONLINE_PRO_001', 'Online pro package', 180000, 0, 3900, 'CNY', 1, 13, 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `token_amount` = VALUES(`token_amount`),
  `bonus_tokens` = VALUES(`bonus_tokens`),
  `price_cent` = VALUES(`price_cent`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_offline_membership_product` (`sku_code`, `name`, `duration_days`, `price_cent`, `currency`, `description`, `tag`, `status`, `sort`, `creator`, `updater`)
VALUES
  ('OFFLINE_SANDBOX_001', 'Offline membership sandbox test', 1, 1, 'CNY', 'Alipay sandbox 0.01 CNY test product', 'test', 1, 1, 'seed', 'seed'),
  ('OFFLINE_TRIAL_1D', 'Offline trial membership', 1, 1, 'CNY', 'One day offline model download trial', 'trial', 1, 10, 'seed', 'seed'),
  ('OFFLINE_MONTHLY_30D', 'Offline monthly membership', 30, 1900, 'CNY', 'Offline model download permission for 30 days', '', 1, 20, 'seed', 'seed'),
  ('OFFLINE_ANNUAL_365D', 'Offline annual membership', 365, 9900, 'CNY', 'Offline model download permission for one year', 'recommended', 1, 30, 'seed', 'seed'),
  ('OFFLINE_PERMANENT', 'Offline permanent membership', 0, 19900, 'CNY', 'Permanent offline model download permission', 'permanent', 1, 40, 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `duration_days` = VALUES(`duration_days`),
  `price_cent` = VALUES(`price_cent`),
  `currency` = VALUES(`currency`),
  `description` = VALUES(`description`),
  `tag` = VALUES(`tag`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_integration_secret` (`config_key`, `config_value`, `encrypted`, `remark`, `creator`, `updater`)
VALUES ('alipay.seller-id', '2088541926073054', 0, 'Alipay seller id / PID', 'admin', 'admin')
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `encrypted` = VALUES(`encrypted`),
  `remark` = VALUES(`remark`),
  `updater` = 'admin',
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_integration_secret` (`config_key`, `config_value`, `encrypted`, `remark`, `creator`, `updater`)
VALUES
  ('tencent.cos.bucket', 'kqtranslate-1300276385', 0, 'Tencent COS model bucket', 'admin', 'admin'),
  ('tencent.cos.region', 'ap-shanghai', 0, 'Tencent COS model region', 'admin', 'admin'),
  ('tencent.cos.endpoint', 'https://cos.ap-shanghai.myqcloud.com', 0, 'Tencent COS model endpoint', 'admin', 'admin'),
  ('tencent.cos.domain', 'https://kqtranslate-1300276385.cos.ap-shanghai.myqcloud.com', 0, 'Tencent COS model public domain', 'admin', 'admin'),
  ('tencent.cos.prefix', 'model-repo/cn/1.0.0/', 0, 'Tencent COS model object prefix', 'admin', 'admin'),
  ('tencent.cos.signed-url-ttl-seconds', '3600', 0, 'Tencent COS signed URL TTL seconds', 'admin', 'admin')
ON DUPLICATE KEY UPDATE
  `config_value` = VALUES(`config_value`),
  `encrypted` = VALUES(`encrypted`),
  `remark` = VALUES(`remark`),
  `updater` = 'admin',
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `infra_config` (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `updater`)
SELECT 'sms', 2, _utf8mb4 0xE79FADE4BFA1E9AA8CE8AF81E7A081E6AF8FE697A5E58F91E98081E6ACA1E695B0,
       'yudao.sms-code.send-maximum-quantity-per-day', '30', b'1',
       _utf8mb4 0xE58D95E4B8AAE6898BE69CBAE58FB7E6AF8FE5A4A9E58FAFE58F91E98081E9AA8CE8AF81E7A081E6ACA1E695B0EFBC8CE9BB98E8AEA420333020E6ACA1,
       'admin', 'admin'
WHERE NOT EXISTS (
  SELECT 1 FROM `infra_config`
  WHERE `config_key` = 'yudao.sms-code.send-maximum-quantity-per-day' AND `deleted` = b'0'
);

INSERT INTO `app_integration_secret` (`config_key`, `config_value`, `encrypted`, `remark`, `creator`, `updater`)
SELECT `config_key`, `config_value`, `encrypted`, `remark`, `creator`, `updater`
FROM (
  SELECT 'tencent.cos.secret-id' AS `config_key`, `config_value`, `encrypted`,
         'Tencent COS secret id' AS `remark`, 'admin' AS `creator`, 'admin' AS `updater`
  FROM `app_integration_secret`
  WHERE `config_key` = 'tencent.sms.secret-id' AND `deleted` = 0 AND `config_value` IS NOT NULL AND `config_value` <> ''
  LIMIT 1
) AS src
WHERE NOT EXISTS (
  SELECT 1 FROM `app_integration_secret` WHERE `config_key` = 'tencent.cos.secret-id' AND `deleted` = 0
);

INSERT INTO `app_integration_secret` (`config_key`, `config_value`, `encrypted`, `remark`, `creator`, `updater`)
SELECT `config_key`, `config_value`, `encrypted`, `remark`, `creator`, `updater`
FROM (
  SELECT 'tencent.cos.secret-key' AS `config_key`, `config_value`, `encrypted`,
         'Tencent COS secret key' AS `remark`, 'admin' AS `creator`, 'admin' AS `updater`
  FROM `app_integration_secret`
  WHERE `config_key` = 'tencent.sms.secret-key' AND `deleted` = 0 AND `config_value` IS NOT NULL AND `config_value` <> ''
  LIMIT 1
) AS src
WHERE NOT EXISTS (
  SELECT 1 FROM `app_integration_secret` WHERE `config_key` = 'tencent.cos.secret-key' AND `deleted` = 0
);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (6900, '支付短信配置', 'app:integration-config:query', 2, 9, 2, 'payment-sms-config', 'ep:key', 'infra/paymentSmsConfig/index', 'InfraPaymentSmsConfig', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
  (6901, '配置查询', 'app:integration-config:query', 3, 1, 6900, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
  (6902, '配置保存', 'app:integration-config:update', 3, 2, 6900, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = b'0';

UPDATE `system_menu` SET `name` = _utf8mb4 0xE694AFE4BB98E79FADE4BFA1E9858DE7BDAE WHERE `id` = 6900;
UPDATE `system_menu` SET `name` = _utf8mb4 0xE694AFE4BB98E79FADE4BFA1E4B88EE6A8A1E59E8BE5AD98E582A8E9858DE7BDAE WHERE `id` = 6900;
UPDATE `system_menu` SET `name` = _utf8mb4 0xE9858DE7BDAEE69FA5E8AFA2 WHERE `id` = 6901;
UPDATE `system_menu` SET `name` = _utf8mb4 0xE9858DE7BDAEE4BF9DE5AD98 WHERE `id` = 6902;

SET @translation_parent_id := (
  SELECT `id` FROM `system_menu`
  WHERE `path` = '/translation' AND `parent_id` = 0 AND `deleted` = b'0'
  ORDER BY `id`
  LIMIT 1
);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 6800, _utf8mb4 0xE7BFBBE8AF91E8BF90E890A5, '', 1, -2, 0, '/translation', 'lucide:languages', NULL, NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'
WHERE @translation_parent_id IS NULL
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = b'0';

SET @translation_parent_id := COALESCE(@translation_parent_id, 6800);

INSERT INTO `system_menu` (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (6903, _utf8mb4 0xE99B86E68890E9858DE7BDAE, 'app:integration-config:query', 2, 90, @translation_parent_id, 'integration-config', 'lucide:key-round', 'infra/paymentSmsConfig/index', 'InfraPaymentSmsConfig', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
  (6904, _utf8mb4 0xE9858DE7BDAEE69FA5E8AFA2, 'app:integration-config:query', 3, 1, 6903, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0'),
  (6905, _utf8mb4 0xE9858DE7BDAEE4BF9DE5AD98, 'app:integration-config:update', 3, 2, 6903, '', '', '', NULL, 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), b'0')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `permission` = VALUES(`permission`),
  `type` = VALUES(`type`),
  `sort` = VALUES(`sort`),
  `parent_id` = VALUES(`parent_id`),
  `path` = VALUES(`path`),
  `icon` = VALUES(`icon`),
  `component` = VALUES(`component`),
  `component_name` = VALUES(`component_name`),
  `status` = VALUES(`status`),
  `visible` = VALUES(`visible`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = b'0';

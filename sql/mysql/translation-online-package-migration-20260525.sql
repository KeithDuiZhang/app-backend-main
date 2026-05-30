USE `ruoyi-vue-pro`;

SET NAMES utf8mb4;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_product' AND column_name = 'text_chars');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_product` ADD COLUMN `text_chars` INT NOT NULL DEFAULT 0 COMMENT ''文本翻译字符数'' AFTER `bonus_tokens`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_product' AND column_name = 'image_translate_count');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_product` ADD COLUMN `image_translate_count` INT NOT NULL DEFAULT 0 COMMENT ''拍照译图张数'' AFTER `text_chars`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_product' AND column_name = 'ocr_translate_count');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_product` ADD COLUMN `ocr_translate_count` INT NOT NULL DEFAULT 0 COMMENT ''文字识别翻译次数'' AFTER `image_translate_count`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_product' AND column_name = 'asr_seconds');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_product` ADD COLUMN `asr_seconds` INT NOT NULL DEFAULT 0 COMMENT ''语音识别秒数'' AFTER `ocr_translate_count`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_product' AND column_name = 'tts_chars');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_product` ADD COLUMN `tts_chars` INT NOT NULL DEFAULT 0 COMMENT ''语音合成朗读字符数'' AFTER `asr_seconds`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_product' AND column_name = 'trial_once');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_product` ADD COLUMN `trial_once` TINYINT NOT NULL DEFAULT 0 COMMENT ''每账号仅一次'' AFTER `tts_chars`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'text_chars_remaining');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `text_chars_remaining` INT NOT NULL DEFAULT 0 COMMENT ''剩余文本字符数'' AFTER `frozen_tokens`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'image_translate_remaining');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `image_translate_remaining` INT NOT NULL DEFAULT 0 COMMENT ''剩余拍照译图张数'' AFTER `text_chars_remaining`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'ocr_translate_remaining');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `ocr_translate_remaining` INT NOT NULL DEFAULT 0 COMMENT ''剩余文字识别翻译次数'' AFTER `image_translate_remaining`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'asr_seconds_remaining');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `asr_seconds_remaining` INT NOT NULL DEFAULT 0 COMMENT ''剩余语音识别秒数'' AFTER `ocr_translate_remaining`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'tts_chars_remaining');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `tts_chars_remaining` INT NOT NULL DEFAULT 0 COMMENT ''剩余朗读字符数'' AFTER `asr_seconds_remaining`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'trial_claimed');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `trial_claimed` TINYINT NOT NULL DEFAULT 0 COMMENT ''是否已领免费体验'' AFTER `tts_chars_remaining`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_token_account' AND column_name = 'current_package_name');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_token_account` ADD COLUMN `current_package_name` VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''最近开通在线套餐名称'' AFTER `trial_claimed`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `app_online_quota_ledger` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水编号',
  `account_id` BIGINT UNSIGNED NOT NULL COMMENT '账户编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `direction` VARCHAR(16) NOT NULL COMMENT 'income/outcome/refund',
  `text_chars_delta` INT NOT NULL DEFAULT 0 COMMENT '文本字符变动',
  `image_translate_delta` INT NOT NULL DEFAULT 0 COMMENT '拍照译图变动',
  `ocr_translate_delta` INT NOT NULL DEFAULT 0 COMMENT '文字识别变动',
  `asr_seconds_delta` INT NOT NULL DEFAULT 0 COMMENT '语音识别秒数变动',
  `tts_chars_delta` INT NOT NULL DEFAULT 0 COMMENT '朗读字符变动',
  `text_chars_after` INT NOT NULL DEFAULT 0 COMMENT '文本字符余额',
  `image_translate_after` INT NOT NULL DEFAULT 0 COMMENT '拍照译图余额',
  `ocr_translate_after` INT NOT NULL DEFAULT 0 COMMENT '文字识别余额',
  `asr_seconds_after` INT NOT NULL DEFAULT 0 COMMENT '语音识别秒数余额',
  `tts_chars_after` INT NOT NULL DEFAULT 0 COMMENT '朗读字符余额',
  `business_type` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '业务类型',
  `business_id` VARCHAR(96) NOT NULL DEFAULT '' COMMENT '业务编号/幂等键',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_online_quota_business` (`user_id`, `direction`, `business_id`, `deleted`),
  KEY `idx_online_quota_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 在线套餐五类额度流水';

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_translation_record' AND column_name = 'text_chars_used');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_translation_record` ADD COLUMN `text_chars_used` INT NOT NULL DEFAULT 0 COMMENT ''文本字符使用量'' AFTER `cost_tokens`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_translation_record' AND column_name = 'image_translate_count_used');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_translation_record` ADD COLUMN `image_translate_count_used` INT NOT NULL DEFAULT 0 COMMENT ''拍照译图使用量'' AFTER `text_chars_used`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_translation_record' AND column_name = 'ocr_translate_count_used');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_translation_record` ADD COLUMN `ocr_translate_count_used` INT NOT NULL DEFAULT 0 COMMENT ''文字识别使用量'' AFTER `image_translate_count_used`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_translation_record' AND column_name = 'asr_seconds_used');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_translation_record` ADD COLUMN `asr_seconds_used` INT NOT NULL DEFAULT 0 COMMENT ''语音识别秒数使用量'' AFTER `ocr_translate_count_used`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_translation_record' AND column_name = 'tts_chars_used');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_translation_record` ADD COLUMN `tts_chars_used` INT NOT NULL DEFAULT 0 COMMENT ''朗读字符使用量'' AFTER `asr_seconds_used`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'app_translation_record' AND column_name = 'client_request_id');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE `app_translation_record` ADD COLUMN `client_request_id` VARCHAR(96) NOT NULL DEFAULT '''' COMMENT ''客户端幂等键'' AFTER `tts_chars_used`', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO `app_token_product` (`sku_code`, `name`, `token_amount`, `bonus_tokens`, `text_chars`, `image_translate_count`, `ocr_translate_count`, `asr_seconds`, `tts_chars`, `trial_once`, `price_cent`, `currency`, `status`, `sort`, `creator`, `updater`)
VALUES
  ('FREE_TRIAL_ONCE', '免费体验', 3000, 0, 3000, 1, 1, 120, 1000, 1, 0, 'CNY', 1, 1, 'migration', 'migration'),
  ('ONLINE_LITE_MONTHLY', '轻量包/月', 50000, 0, 50000, 15, 25, 3000, 60000, 0, 3900, 'CNY', 1, 10, 'migration', 'migration'),
  ('ONLINE_STANDARD_MONTHLY', '标准包/月', 250000, 0, 250000, 60, 100, 12000, 300000, 0, 15900, 'CNY', 1, 20, 'migration', 'migration'),
  ('ONLINE_PRO_MONTHLY', '专业包/月', 1000000, 0, 1000000, 250, 400, 48000, 1200000, 0, 59900, 'CNY', 1, 30, 'migration', 'migration'),
  ('ONLINE_SUPER_MONTHLY', '超级包/月', 5000000, 0, 5000000, 1250, 2000, 240000, 6000000, 0, 289900, 'CNY', 1, 40, 'migration', 'migration')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `token_amount` = VALUES(`token_amount`),
  `text_chars` = VALUES(`text_chars`),
  `image_translate_count` = VALUES(`image_translate_count`),
  `ocr_translate_count` = VALUES(`ocr_translate_count`),
  `asr_seconds` = VALUES(`asr_seconds`),
  `tts_chars` = VALUES(`tts_chars`),
  `trial_once` = VALUES(`trial_once`),
  `price_cent` = VALUES(`price_cent`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

UPDATE `app_token_product`
SET `status` = 0, `update_time` = CURRENT_TIMESTAMP
WHERE `sku_code` IN ('TOKEN_SANDBOX_001', 'TOKEN_300', 'TOKEN_1000', 'TOKEN_3000',
                     'ONLINE_LITE_001', 'ONLINE_STANDARD_001', 'ONLINE_PRO_001')
  AND `deleted` = 0;

UPDATE `app_offline_membership_product`
SET `price_cent` = 100, `name` = '1 天离线体验', `description` = '体验离线模型下载和断网翻译能力', `tag` = 'trial', `status` = 1
WHERE `sku_code` IN ('OFFLINE_TRIAL_1D', 'OFFLINE_SANDBOX_001') AND `deleted` = 0;

UPDATE `app_offline_membership_product`
SET `price_cent` = 1900, `name` = '月度离线会员', `duration_days` = 30, `status` = 1
WHERE `sku_code` = 'OFFLINE_MONTHLY_30D' AND `deleted` = 0;

UPDATE `app_offline_membership_product`
SET `price_cent` = 9900, `name` = '年度离线会员', `duration_days` = 365,
    `description` = '一年内使用离线能力，可下载常用模型包', `tag` = '推荐', `status` = 1
WHERE `sku_code` = 'OFFLINE_ANNUAL_365D' AND `deleted` = 0;

UPDATE `app_offline_membership_product`
SET `price_cent` = 19900, `name` = '永久离线会员', `duration_days` = 0,
    `description` = '永久使用已支持离线模型下载和本机翻译能力', `tag` = '永久', `status` = 1
WHERE `sku_code` = 'OFFLINE_PERMANENT' AND `deleted` = 0;

INSERT INTO `app_offline_membership_product`
(`sku_code`, `name`, `duration_days`, `price_cent`, `currency`, `description`, `tag`, `status`, `sort`, `creator`, `updater`)
VALUES
('OFFLINE_TRIAL_1D', '1 天离线体验', 1, 100, 'CNY', '开通后可下载基础离线模型，适合上线前体验', '体验', 1, 10, 'migration', 'migration'),
('OFFLINE_MONTHLY_30D', '月度离线会员', 30, 1900, 'CNY', '离线文本翻译、拍照识别、语音识别和播报模型下载权限', '月度', 1, 20, 'migration', 'migration'),
('OFFLINE_ANNUAL_365D', '年度离线会员', 365, 9900, 'CNY', '一年内使用离线能力，可下载常用模型包', '推荐', 1, 30, 'migration', 'migration'),
('OFFLINE_PERMANENT', '永久离线会员', 0, 19900, 'CNY', '永久使用已支持离线模型下载和本机翻译能力', '永久', 1, 40, 'migration', 'migration')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `duration_days` = VALUES(`duration_days`),
  `price_cent` = VALUES(`price_cent`),
  `currency` = VALUES(`currency`),
  `description` = VALUES(`description`),
  `tag` = VALUES(`tag`),
  `status` = VALUES(`status`),
  `sort` = VALUES(`sort`),
  `updater` = 'migration',
  `update_time` = NOW();

UPDATE `app_offline_membership_product`
SET `status` = 0, `updater` = 'migration', `update_time` = NOW()
WHERE `sku_code` NOT IN ('OFFLINE_TRIAL_1D', 'OFFLINE_MONTHLY_30D', 'OFFLINE_ANNUAL_365D', 'OFFLINE_PERMANENT')
  AND `deleted` = 0;

UPDATE `app_payment_order`
SET `product_type` = 'online_package'
WHERE `product_type` = 'token' AND `deleted` = 0;

UPDATE `app_payment_order`
SET `product_type` = 'offline_membership'
WHERE `product_type` = 'offline_vip' AND `deleted` = 0;

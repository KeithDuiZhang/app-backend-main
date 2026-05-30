USE `ruoyi-vue-pro`;

SET NAMES utf8mb4;

INSERT INTO `app_user` (`app_user_no`, `nickname`, `avatar_url`, `phone_cipher`, `email`, `auth_status`, `status`, `source_channel`, `last_login_at`, `creator`, `updater`)
VALUES
  ('test_user_001', '测试用户', '', '138****0000', 'tester@example.invalid', 10, 1, 'android', NOW(), 'seed', 'seed'),
  ('android_demo_19912341234', '演示用户', '', '19912341234', 'demo@example.invalid', 10, 1, 'android', NOW(), 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `nickname` = VALUES(`nickname`),
  `auth_status` = VALUES(`auth_status`),
  `status` = VALUES(`status`),
  `last_login_at` = VALUES(`last_login_at`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_user_identity` (`user_id`, `identity_type`, `provider_open_id`, `credential_hash`, `enabled`, `creator`, `updater`)
SELECT `id`, 'password', 'test_user_001', 'reserved-test-password-hash', 1, 'seed', 'seed'
FROM `app_user`
WHERE `app_user_no` = 'test_user_001'
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_user_identity` (`user_id`, `identity_type`, `provider_open_id`, `credential_hash`, `enabled`, `creator`, `updater`)
SELECT `id`, 'password', '19912341234', '$2a$10$wwRsqcz.ihkz81msH5E.QOIGf0abEQbrqbU6GQ0p2wdFZs5e/qz36', 1, 'seed', 'seed'
FROM `app_user`
WHERE `app_user_no` = 'android_demo_19912341234'
ON DUPLICATE KEY UPDATE
  `credential_hash` = VALUES(`credential_hash`),
  `enabled` = VALUES(`enabled`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_device` (`user_id`, `device_uuid`, `platform`, `app_version`, `device_model`, `os_version`, `network_policy`, `last_seen_at`, `creator`, `updater`)
SELECT `id`, 'android-test-ca13e375d6ed', 'android', 'v8-smoke', 'ADB device ca13e375d6ed', 'Android', 'online_first', NOW(), 'seed', 'seed'
FROM `app_user`
WHERE `app_user_no` = 'test_user_001'
ON DUPLICATE KEY UPDATE
  `app_version` = VALUES(`app_version`),
  `network_policy` = VALUES(`network_policy`),
  `last_seen_at` = VALUES(`last_seen_at`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_token_account` (`user_id`, `balance_tokens`, `trial_tokens`, `frozen_tokens`, `creator`, `updater`)
SELECT `id`, 300, 300, 0, 'seed', 'seed'
FROM `app_user`
WHERE `app_user_no` = 'test_user_001'
ON DUPLICATE KEY UPDATE
  `balance_tokens` = VALUES(`balance_tokens`),
  `trial_tokens` = VALUES(`trial_tokens`),
  `frozen_tokens` = VALUES(`frozen_tokens`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_token_account` (`user_id`, `balance_tokens`, `trial_tokens`, `frozen_tokens`, `creator`, `updater`)
SELECT `id`, 80000, 3000, 0, 'seed', 'seed'
FROM `app_user`
WHERE `app_user_no` = 'android_demo_19912341234'
ON DUPLICATE KEY UPDATE
  `trial_tokens` = VALUES(`trial_tokens`),
  `frozen_tokens` = VALUES(`frozen_tokens`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_token_ledger` (`account_id`, `user_id`, `direction`, `change_tokens`, `balance_after`, `business_type`, `business_id`, `remark`, `creator`, `updater`)
SELECT a.`id`, a.`user_id`, 'income', 300, 300, 'trial_grant', 'seed-20260520', '测试阶段体验 Token', 'seed', 'seed'
FROM `app_token_account` a
JOIN `app_user` u ON u.`id` = a.`user_id`
WHERE u.`app_user_no` = 'test_user_001'
  AND NOT EXISTS (
    SELECT 1 FROM `app_token_ledger` l
    WHERE l.`account_id` = a.`id` AND l.`business_id` = 'seed-20260520' AND l.`deleted` = 0
  );

INSERT INTO `app_token_product` (`sku_code`, `name`, `token_amount`, `bonus_tokens`, `price_cent`, `currency`, `status`, `sort`, `creator`, `updater`)
VALUES
  ('TOKEN_SANDBOX_001', '支付宝沙箱测试包', 1, 0, 1, 'CNY', 1, 1, 'seed', 'seed'),
  ('ONLINE_LITE_001', '轻量包', 35000, 0, 1, 'CNY', 1, 11, 'seed', 'seed'),
  ('ONLINE_STANDARD_001', '常用包', 80000, 0, 1900, 'CNY', 1, 12, 'seed', 'seed'),
  ('ONLINE_PRO_001', '畅用包', 180000, 0, 3900, 'CNY', 1, 13, 'seed', 'seed'),
  ('TOKEN_300', '300 Token', 300, 0, 1900, 'CNY', 1, 10, 'seed', 'seed'),
  ('TOKEN_1000', '1000 Token', 1000, 120, 4900, 'CNY', 1, 20, 'seed', 'seed'),
  ('TOKEN_3000', '3000 Token', 3000, 500, 9900, 'CNY', 1, 30, 'seed', 'seed')
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
  ('OFFLINE_SANDBOX_001', '离线会员沙箱测试', 1, 1, 'CNY', '支付宝沙箱 0.01 元测试商品', 'test', 1, 1, 'seed', 'seed'),
  ('OFFLINE_TRIAL_1D', '1 天离线体验', 1, 1, 'CNY', '仅开放基础模型包下载，适合先试用', 'trial', 1, 10, 'seed', 'seed'),
  ('OFFLINE_MONTHLY_30D', '月度离线会员', 30, 1900, 'CNY', '30 天离线模型下载权限', '', 1, 20, 'seed', 'seed'),
  ('OFFLINE_ANNUAL_365D', '年度离线会员', 365, 9900, 'CNY', '一年内使用离线能力，可下载常用模型包', 'recommended', 1, 30, 'seed', 'seed'),
  ('OFFLINE_PERMANENT', '永久离线会员', 0, 19900, 'CNY', '永久使用离线能力，可下载已支持的模型', 'permanent', 1, 40, 'seed', 'seed')
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

INSERT INTO `app_payment_order` (`order_no`, `user_id`, `product_type`, `product_id`, `pay_channel`, `status`, `amount_cent`, `provider_trade_no`, `paid_at`, `creator`, `updater`)
SELECT 'PAY-SEED-20260520-001', u.`id`, 'token', p.`id`, 'reserved', 'paid', 1900, '', NOW(), 'seed', 'seed'
FROM `app_user` u
JOIN `app_token_product` p ON p.`sku_code` = 'TOKEN_300'
WHERE u.`app_user_no` = 'test_user_001'
ON DUPLICATE KEY UPDATE
  `status` = VALUES(`status`),
  `amount_cent` = VALUES(`amount_cent`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_offline_membership` (`user_id`, `plan_code`, `status`, `started_at`, `expired_at`, `source_order_no`, `creator`, `updater`)
SELECT u.`id`, 'OFFLINE_VIP_TEST_30D', 'active', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'PAY-SEED-20260520-001', 'seed', 'seed'
FROM `app_user` u
WHERE u.`app_user_no` = 'test_user_001'
  AND NOT EXISTS (
    SELECT 1 FROM `app_offline_membership` m
    WHERE m.`user_id` = u.`id` AND m.`plan_code` = 'OFFLINE_VIP_TEST_30D' AND m.`deleted` = 0
  );

INSERT INTO `app_language_pack` (`language_code`, `language_name_zh`, `language_name_en`, `pack_type`, `engine_group`, `version`, `status`, `size_bytes`, `sha256`, `local_path_hint`, `min_ram_mb`, `device_advice`, `verified_at`, `creator`, `updater`)
VALUES
  ('zh', '中文', 'Chinese', 'translation', 'hymt', 'verified-20260520', 'builtin', 0, '', 'assets/models/hymt', 2048, '默认内置', NOW(), 'seed', 'seed'),
  ('en', '英语', 'English', 'translation', 'hymt', 'verified-20260520', 'builtin', 0, '', 'assets/models/hymt', 2048, '默认内置', NOW(), 'seed', 'seed'),
  ('ja', '日语', 'Japanese', 'asr', 'sensevoice', 'verified-20260520', 'builtin', 0, '', 'assets/models/sensevoice', 2048, '默认内置', NOW(), 'seed', 'seed'),
  ('ko', '韩语', 'Korean', 'asr', 'sensevoice', 'verified-20260520', 'builtin', 0, '', 'assets/models/sensevoice', 2048, '默认内置', NOW(), 'seed', 'seed'),
  ('ru', '俄语', 'Russian', 'ocr', 'tesseract4android', 'verified-20260520', 'downloadable', 0, '', 'external/tessdata/rus.traineddata', 2048, '模型安装后展示', NOW(), 'seed', 'seed'),
  ('ar', '阿拉伯语', 'Arabic', 'ocr', 'tesseract4android', 'experimental-20260520', 'not_recommended', 0, '', 'external/tessdata/ara.traineddata', 4096, '实验支持，不默认推荐', NULL, 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `language_name_zh` = VALUES(`language_name_zh`),
  `language_name_en` = VALUES(`language_name_en`),
  `status` = VALUES(`status`),
  `device_advice` = VALUES(`device_advice`),
  `verified_at` = VALUES(`verified_at`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_capability_matrix` (`mode`, `scene`, `source_language_code`, `target_language_code`, `status`, `provider`, `display_badge`, `show_in_app`, `tts_available`, `priority`, `notes`, `verified_at`, `creator`, `updater`)
VALUES
  ('offline', 'text', 'zh', 'en', 'verified', 'hymt', '离线可用', 1, 1, 10, 'HY-MT 已完成真机方向验收基线的一部分', NOW(), 'seed', 'seed'),
  ('offline', 'text', 'en', 'zh', 'verified', 'hymt', '离线可用', 1, 1, 11, 'HY-MT 已完成真机方向验收基线的一部分', NOW(), 'seed', 'seed'),
  ('offline', 'asr', 'zh', '', 'verified', 'sensevoice', '已验收', 1, 0, 20, 'SenseVoice core 已验收', NOW(), 'seed', 'seed'),
  ('offline', 'asr', 'en', '', 'verified', 'sensevoice', '已验收', 1, 0, 21, 'SenseVoice core 已验收', NOW(), 'seed', 'seed'),
  ('offline', 'ocr', 'ru', '', 'verified', 'tesseract4android', '需安装语言包', 1, 0, 30, 'Tesseract 扩展模型安装后可用', NOW(), 'seed', 'seed'),
  ('offline', 'ocr', 'ar', '', 'experimental', 'tesseract4android', '实验支持', 1, 0, 31, '不得展示为稳定已支持', NULL, 'seed', 'seed'),
  ('online', 'text', 'zh', 'en', 'verified', 'aliyun', '在线可用', 1, 1, 40, '测试数据，真实上线以阿里云验收矩阵为准', NOW(), 'seed', 'seed'),
  ('online', 'tts', 'en', '', 'verified', 'aliyun', '可朗读', 1, 1, 41, '测试数据，真实上线以阿里云验收矩阵为准', NOW(), 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `status` = VALUES(`status`),
  `display_badge` = VALUES(`display_badge`),
  `show_in_app` = VALUES(`show_in_app`),
  `tts_available` = VALUES(`tts_available`),
  `priority` = VALUES(`priority`),
  `notes` = VALUES(`notes`),
  `verified_at` = VALUES(`verified_at`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_translation_record` (`user_id`, `device_uuid`, `mode`, `scene`, `source_language_code`, `target_language_code`, `source_text`, `translated_text`, `engine_provider`, `cost_tokens`, `status`, `creator`, `updater`)
SELECT u.`id`, 'android-test-ca13e375d6ed', 'offline', 'text', 'zh', 'en', '你好，世界', 'Hello, world', 'hymt', 1, 'success', 'seed', 'seed'
FROM `app_user` u
WHERE u.`app_user_no` = 'test_user_001'
  AND NOT EXISTS (
    SELECT 1 FROM `app_translation_record` r
    WHERE r.`device_uuid` = 'android-test-ca13e375d6ed'
      AND r.`source_text` = '你好，世界'
      AND r.`deleted` = 0
  );

INSERT INTO `app_favorite` (`user_id`, `record_id`, `title`, `source_language_code`, `target_language_code`, `source_text`, `translated_text`, `creator`, `updater`)
SELECT u.`id`, r.`id`, '出行常用语', 'zh', 'en', r.`source_text`, r.`translated_text`, 'seed', 'seed'
FROM `app_user` u
JOIN `app_translation_record` r ON r.`user_id` = u.`id` AND r.`source_text` = '你好，世界'
WHERE u.`app_user_no` = 'test_user_001'
  AND NOT EXISTS (
    SELECT 1 FROM `app_favorite` f
    WHERE f.`user_id` = u.`id` AND f.`title` = '出行常用语' AND f.`deleted` = 0
  );

INSERT INTO `app_feedback` (`user_id`, `device_uuid`, `category`, `content`, `contact`, `status`, `handler_remark`, `creator`, `updater`)
SELECT u.`id`, 'android-test-ca13e375d6ed', 'ui', '测试阶段反馈：检查 Prototype5 视觉对齐和离线语言包入口。', 'tester@example.invalid', 'open', '', 'seed', 'seed'
FROM `app_user` u
WHERE u.`app_user_no` = 'test_user_001'
  AND NOT EXISTS (
    SELECT 1 FROM `app_feedback` f
    WHERE f.`user_id` = u.`id` AND f.`category` = 'ui' AND f.`deleted` = 0
  );

INSERT INTO `app_content` (`content_key`, `title`, `content_body`, `scene`, `status`, `creator`, `updater`)
VALUES
  ('recharge_notice', 'Token 使用说明', 'Token 用于在线翻译、图片识别和语音能力消耗；测试阶段支付渠道仅保留配置入口。', 'recharge', 1, 'seed', 'seed'),
  ('offline_vip_notice', '离线会员说明', '离线会员用于管理本地语言包和离线能力；未验收能力不得展示为稳定可用。', 'offline_vip', 1, 'seed', 'seed'),
  ('privacy_summary', '隐私说明摘要', '测试阶段不接入真实微信、支付宝和短信服务，不保存真实第三方密钥。', 'policy', 1, 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `title` = VALUES(`title`),
  `content_body` = VALUES(`content_body`),
  `scene` = VALUES(`scene`),
  `status` = VALUES(`status`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

INSERT INTO `app_sms_verification` (`phone_cipher`, `purpose`, `code_hash`, `status`, `expires_at`, `creator`, `updater`)
VALUES
  ('138****0000', 'login', 'reserved-disabled-in-test', 'reserved', DATE_ADD(NOW(), INTERVAL 10 MINUTE), 'seed', 'seed');

INSERT INTO `app_external_auth_config` (`provider`, `enabled`, `config_json`, `remark`, `creator`, `updater`)
VALUES
  ('wechat', 0, JSON_OBJECT('reserved', true, 'secretConfigured', false), '测试阶段暂不接入微信登录', 'seed', 'seed'),
  ('alipay', 0, JSON_OBJECT('reserved', true, 'secretConfigured', false), '测试阶段暂不接入支付宝支付', 'seed', 'seed'),
  ('sms', 0, JSON_OBJECT('reserved', true, 'secretConfigured', false), '测试阶段暂不接入短信验证', 'seed', 'seed')
ON DUPLICATE KEY UPDATE
  `enabled` = VALUES(`enabled`),
  `config_json` = VALUES(`config_json`),
  `remark` = VALUES(`remark`),
  `update_time` = CURRENT_TIMESTAMP,
  `deleted` = 0;

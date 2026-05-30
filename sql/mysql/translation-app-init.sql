CREATE DATABASE IF NOT EXISTS `ruoyi-vue-pro`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ruoyi-vue-pro`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `app_user` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `app_user_no` VARCHAR(64) NOT NULL COMMENT 'App 用户号',
  `nickname` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '头像',
  `phone_cipher` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '手机号密文或脱敏值',
  `email` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '邮箱',
  `auth_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态：0 未认证，10 测试账号，20 已认证',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 禁用',
  `source_channel` VARCHAR(32) NOT NULL DEFAULT 'android' COMMENT '来源渠道',
  `last_login_at` DATETIME NULL COMMENT '最近登录时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_user_no` (`app_user_no`),
  KEY `idx_phone_cipher` (`phone_cipher`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 用户';

CREATE TABLE IF NOT EXISTS `app_user_identity` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '身份编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `identity_type` VARCHAR(32) NOT NULL COMMENT '身份类型：password/wechat/alipay/sms',
  `provider_open_id` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '第三方 OpenId，测试阶段为空',
  `provider_union_id` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '第三方 UnionId，测试阶段为空',
  `credential_hash` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '凭证摘要',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity_provider` (`identity_type`, `provider_open_id`, `deleted`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 用户身份绑定';

CREATE TABLE IF NOT EXISTS `app_device` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '设备编号',
  `user_id` BIGINT UNSIGNED NULL COMMENT '用户编号',
  `device_uuid` VARCHAR(128) NOT NULL COMMENT '设备唯一标识',
  `platform` VARCHAR(32) NOT NULL DEFAULT 'android' COMMENT '平台',
  `app_version` VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'App 版本',
  `device_model` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '设备型号',
  `os_version` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '系统版本',
  `network_policy` VARCHAR(32) NOT NULL DEFAULT 'online_first' COMMENT '翻译模式偏好',
  `last_seen_at` DATETIME NULL COMMENT '最近活跃时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_uuid` (`device_uuid`, `deleted`),
  KEY `idx_device_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 设备';

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

CREATE TABLE IF NOT EXISTS `app_token_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Token 账户编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `balance_tokens` INT NOT NULL DEFAULT 0 COMMENT '可用 Token',
  `trial_tokens` INT NOT NULL DEFAULT 0 COMMENT '体验 Token',
  `frozen_tokens` INT NOT NULL DEFAULT 0 COMMENT '冻结 Token',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_account_user` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App Token 账户';

CREATE TABLE IF NOT EXISTS `app_token_ledger` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水编号',
  `account_id` BIGINT UNSIGNED NOT NULL COMMENT '账户编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `direction` VARCHAR(16) NOT NULL COMMENT '方向：income/outcome/freeze/unfreeze',
  `change_tokens` INT NOT NULL COMMENT '变动 Token',
  `balance_after` INT NOT NULL COMMENT '变动后余额',
  `business_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
  `business_id` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务编号',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_token_ledger_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App Token 流水';

CREATE TABLE IF NOT EXISTS `app_token_product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商品编号',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 编码',
  `name` VARCHAR(64) NOT NULL COMMENT '商品名称',
  `token_amount` INT NOT NULL COMMENT '基础 Token 数',
  `bonus_tokens` INT NOT NULL DEFAULT 0 COMMENT '赠送 Token 数',
  `price_cent` INT NOT NULL COMMENT '价格，单位分',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 上架，0 下架',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_token_product_sku` (`sku_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App Token 商品';

CREATE TABLE IF NOT EXISTS `app_offline_membership_product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '离线会员商品编号',
  `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU 编码',
  `name` VARCHAR(64) NOT NULL COMMENT '商品名称',
  `duration_days` INT NOT NULL DEFAULT 0 COMMENT '有效天数，0 表示永久',
  `price_cent` INT NOT NULL COMMENT '价格，单位分',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `description` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '说明',
  `tag` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '展示标签',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 上架，0 下架',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_offline_membership_product_sku` (`sku_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 离线会员商品';

CREATE TABLE IF NOT EXISTS `app_payment_order` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单编号',
  `order_no` VARCHAR(64) NOT NULL COMMENT '业务订单号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `product_type` VARCHAR(32) NOT NULL COMMENT '商品类型：token/offline_vip',
  `product_id` BIGINT UNSIGNED NULL COMMENT '商品编号',
  `pay_channel` VARCHAR(32) NOT NULL DEFAULT 'reserved' COMMENT '支付渠道：reserved/wechat/alipay',
  `status` VARCHAR(32) NOT NULL DEFAULT 'created' COMMENT '订单状态',
  `amount_cent` INT NOT NULL COMMENT '金额，单位分',
  `provider_trade_no` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '第三方交易号',
  `paid_at` DATETIME NULL COMMENT '支付时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_order_no` (`order_no`),
  KEY `idx_payment_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 支付订单预留';

CREATE TABLE IF NOT EXISTS `app_offline_membership` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '离线会员编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `plan_code` VARCHAR(64) NOT NULL COMMENT '套餐编码',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态：active/expired/cancelled',
  `started_at` DATETIME NOT NULL COMMENT '开始时间',
  `expired_at` DATETIME NULL COMMENT '过期时间，空表示永久',
  `source_order_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源订单号',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_offline_membership_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 离线会员';

CREATE TABLE IF NOT EXISTS `app_language_pack` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '语言包编号',
  `language_code` VARCHAR(16) NOT NULL COMMENT '语言代码',
  `language_name_zh` VARCHAR(64) NOT NULL COMMENT '中文名',
  `language_name_en` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '英文名',
  `pack_type` VARCHAR(32) NOT NULL COMMENT '包类型：translation/asr/tts/ocr',
  `engine_group` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '能力组',
  `version` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '版本',
  `status` VARCHAR(32) NOT NULL COMMENT '状态：builtin/installed/downloadable/unsupported/not_recommended',
  `size_bytes` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '大小',
  `sha256` CHAR(64) NOT NULL DEFAULT '' COMMENT 'SHA-256',
  `download_url` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '下载地址',
  `local_path_hint` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '本地路径提示',
  `min_ram_mb` INT NOT NULL DEFAULT 0 COMMENT '建议最低内存',
  `device_advice` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '设备建议',
  `verified_at` DATETIME NULL COMMENT '验收时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_language_pack` (`language_code`, `pack_type`, `engine_group`, `version`, `deleted`),
  KEY `idx_language_pack_status` (`status`, `pack_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 离线语言包';

CREATE TABLE IF NOT EXISTS `app_capability_matrix` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '能力编号',
  `mode` VARCHAR(16) NOT NULL COMMENT '模式：online/offline',
  `scene` VARCHAR(32) NOT NULL COMMENT '场景：text/ocr/asr/tts/image_translation',
  `source_language_code` VARCHAR(16) NOT NULL COMMENT '源语言',
  `target_language_code` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '目标语言，单端能力为空',
  `status` VARCHAR(32) NOT NULL COMMENT '状态：verified/experimental/need_download/unsupported',
  `provider` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '能力提供方或引擎',
  `display_badge` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '前端展示标签',
  `show_in_app` TINYINT NOT NULL DEFAULT 1 COMMENT '是否在 App 展示',
  `tts_available` TINYINT NOT NULL DEFAULT 0 COMMENT '是否可播报',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '展示排序',
  `notes` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `verified_at` DATETIME NULL COMMENT '验收时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_capability` (`mode`, `scene`, `source_language_code`, `target_language_code`, `provider`, `deleted`),
  KEY `idx_capability_show` (`mode`, `scene`, `show_in_app`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 能力矩阵';

CREATE TABLE IF NOT EXISTS `app_translation_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '翻译记录编号',
  `user_id` BIGINT UNSIGNED NULL COMMENT '用户编号',
  `device_uuid` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '设备标识',
  `mode` VARCHAR(16) NOT NULL COMMENT '模式：online/offline/auto_offline',
  `scene` VARCHAR(32) NOT NULL COMMENT '场景：text/camera/conversation',
  `source_language_code` VARCHAR(16) NOT NULL COMMENT '源语言',
  `target_language_code` VARCHAR(16) NOT NULL COMMENT '目标语言',
  `source_text` TEXT NULL COMMENT '原文',
  `translated_text` TEXT NULL COMMENT '译文',
  `engine_provider` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '提供方或引擎',
  `cost_tokens` INT NOT NULL DEFAULT 0 COMMENT '消耗 Token',
  `status` VARCHAR(32) NOT NULL DEFAULT 'success' COMMENT '状态',
  `error_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误码',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_translation_user_time` (`user_id`, `create_time`),
  KEY `idx_translation_lang` (`source_language_code`, `target_language_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 翻译记录';

CREATE TABLE IF NOT EXISTS `app_favorite` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏编号',
  `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户编号',
  `record_id` BIGINT UNSIGNED NULL COMMENT '翻译记录编号',
  `title` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '标题',
  `source_language_code` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '源语言',
  `target_language_code` VARCHAR(16) NOT NULL DEFAULT '' COMMENT '目标语言',
  `source_text` TEXT NULL COMMENT '原文',
  `translated_text` TEXT NULL COMMENT '译文',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_favorite_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 收藏';

CREATE TABLE IF NOT EXISTS `app_feedback` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '反馈编号',
  `user_id` BIGINT UNSIGNED NULL COMMENT '用户编号',
  `device_uuid` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '设备标识',
  `category` VARCHAR(32) NOT NULL COMMENT '反馈类型',
  `content` TEXT NOT NULL COMMENT '反馈内容',
  `contact` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '联系方式',
  `status` VARCHAR(32) NOT NULL DEFAULT 'open' COMMENT '处理状态',
  `handler_remark` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '处理备注',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_feedback_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 反馈';

CREATE TABLE IF NOT EXISTS `app_content` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '内容编号',
  `content_key` VARCHAR(64) NOT NULL COMMENT '内容键',
  `title` VARCHAR(128) NOT NULL COMMENT '标题',
  `content_body` TEXT NOT NULL COMMENT '内容',
  `scene` VARCHAR(32) NOT NULL DEFAULT 'app' COMMENT '场景',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 发布，0 草稿',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_key` (`content_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 内容配置';

CREATE TABLE IF NOT EXISTS `app_sms_verification` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '短信验证码编号',
  `phone_cipher` VARCHAR(128) NOT NULL COMMENT '手机号密文或脱敏值',
  `purpose` VARCHAR(32) NOT NULL COMMENT '用途',
  `code_hash` VARCHAR(128) NOT NULL COMMENT '验证码摘要',
  `status` VARCHAR(32) NOT NULL DEFAULT 'reserved' COMMENT '状态：reserved/sent/used/expired',
  `expires_at` DATETIME NULL COMMENT '过期时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_sms_phone_purpose` (`phone_cipher`, `purpose`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 短信验证预留';

CREATE TABLE IF NOT EXISTS `app_external_auth_config` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '配置编号',
  `provider` VARCHAR(32) NOT NULL COMMENT '提供方：wechat/alipay/sms',
  `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用',
  `config_json` JSON NULL COMMENT '配置 JSON，测试阶段不写密钥',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  `creator` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_external_auth_provider` (`provider`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='翻译 App 第三方登录支付短信预留配置';

SET FOREIGN_KEY_CHECKS = 1;

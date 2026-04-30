-- love-match-init.sql
-- 增量初始化：婚恋核心业务表与基础种子数据

CREATE TABLE IF NOT EXISTS `love_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户编号',
  `social_user_id` bigint DEFAULT NULL COMMENT '社交用户编号',
  `openid` varchar(64) DEFAULT NULL COMMENT 'OpenID',
  `unionid` varchar(64) DEFAULT NULL COMMENT 'UnionID',
  `nickname` varchar(64) NOT NULL DEFAULT '' COMMENT '昵称',
  `avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '头像',
  `mobile` varchar(20) DEFAULT NULL COMMENT '手机号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `auth_status` tinyint NOT NULL DEFAULT 0 COMMENT '认证状态',
  `certified_at` datetime DEFAULT NULL COMMENT '认证完成时间',
  `member_level` tinyint NOT NULL DEFAULT 0 COMMENT '会员等级',
  `member_expire_time` datetime DEFAULT NULL COMMENT '会员到期时间',
  `free_match_quota` int NOT NULL DEFAULT 1 COMMENT '免费牵线额度',
  `last_quota_reset_at` datetime DEFAULT NULL COMMENT '最近额度重置时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_love_user_mobile` (`mobile`),
  UNIQUE KEY `uk_love_user_openid` (`openid`),
  KEY `idx_love_user_social_user_id` (`social_user_id`),
  KEY `idx_love_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋用户表';

SET @love_user_member_expire_time_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_user'
    AND COLUMN_NAME = 'member_expire_time'
);
SET @love_user_member_expire_time_sql := IF(
  @love_user_member_expire_time_exists = 0,
  'ALTER TABLE `love_user` ADD COLUMN `member_expire_time` datetime DEFAULT NULL COMMENT ''会员到期时间'' AFTER `member_level`',
  'SELECT 1'
);
PREPARE love_user_member_expire_time_stmt FROM @love_user_member_expire_time_sql;
EXECUTE love_user_member_expire_time_stmt;
DEALLOCATE PREPARE love_user_member_expire_time_stmt;

CREATE TABLE IF NOT EXISTS `love_user_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资料编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `real_name` varchar(32) DEFAULT NULL COMMENT '真实姓名',
  `gender` tinyint NOT NULL DEFAULT 0 COMMENT '性别',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `city_code` varchar(32) DEFAULT NULL COMMENT '城市编码',
  `city_name` varchar(64) DEFAULT NULL COMMENT '城市名称',
  `marital_status` tinyint NOT NULL DEFAULT 0 COMMENT '婚姻状态',
  `height_cm` int DEFAULT NULL COMMENT '身高(cm)',
  `weight_kg` int DEFAULT NULL COMMENT '体重(kg)',
  `profession` varchar(64) DEFAULT NULL COMMENT '职业',
  `education` varchar(64) DEFAULT NULL COMMENT '学历',
  `income_desc` varchar(64) DEFAULT NULL COMMENT '收入描述',
  `photos` text DEFAULT NULL COMMENT '照片列表',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人介绍',
  `tags` varchar(500) DEFAULT NULL COMMENT '标签列表',
  `partner_preference` varchar(500) DEFAULT NULL COMMENT '择偶要求',
  `profile_public` bit(1) NOT NULL DEFAULT b'1' COMMENT '资料是否公开',
  `completion_rate` tinyint NOT NULL DEFAULT 0 COMMENT '资料完成度',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_love_user_profile_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋用户资料表';

CREATE TABLE IF NOT EXISTS `love_auth_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '认证订单编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `pay_order_id` bigint DEFAULT NULL COMMENT '支付订单编号',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `amount` int NOT NULL DEFAULT 0 COMMENT '支付金额(分)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '认证状态',
  `verified_result` varchar(255) DEFAULT NULL COMMENT '认证结果',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `verified_time` datetime DEFAULT NULL COMMENT '认证完成时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_auth_order_user_id` (`user_id`),
  KEY `idx_love_auth_order_pay_order_id` (`pay_order_id`),
  UNIQUE KEY `uk_love_auth_order_order_no` (`order_no`),
  KEY `idx_love_auth_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋认证订单表';

CREATE TABLE IF NOT EXISTS `love_matchmaker` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '红娘编号',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `avatar` varchar(255) NOT NULL DEFAULT '' COMMENT '头像',
  `introduction` varchar(500) DEFAULT NULL COMMENT '简介',
  `mobile` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `wechat_no` varchar(64) DEFAULT NULL COMMENT '微信号',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `is_default` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认红娘',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_matchmaker_default_status` (`is_default`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='红娘表';

CREATE TABLE IF NOT EXISTS `love_match_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '牵线申请编号',
  `from_user_id` bigint NOT NULL COMMENT '发起用户编号',
  `to_user_id` bigint NOT NULL COMMENT '目标用户编号',
  `matchmaker_id` bigint NOT NULL COMMENT '红娘编号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '申请状态',
  `reject_reason` varchar(255) DEFAULT NULL COMMENT '拒绝原因',
  `submitted_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `processed_at` datetime DEFAULT NULL COMMENT '处理时间',
  `apply_reason` varchar(500) DEFAULT NULL COMMENT '申请原因',
  `source_type` tinyint NOT NULL DEFAULT 0 COMMENT '申请来源',
  `latest_log_id` bigint DEFAULT NULL COMMENT '最新日志编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_match_apply_from_to` (`from_user_id`, `to_user_id`),
  KEY `idx_love_match_apply_matchmaker_id` (`matchmaker_id`),
  KEY `idx_love_match_apply_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='牵线申请表';

CREATE TABLE IF NOT EXISTS `love_match_apply_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '牵线申请日志编号',
  `apply_id` bigint NOT NULL COMMENT '申请编号',
  `from_status` tinyint DEFAULT NULL COMMENT '变更前状态',
  `to_status` tinyint DEFAULT NULL COMMENT '变更后状态',
  `operator_type` tinyint NOT NULL DEFAULT 0 COMMENT '操作人类型',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人编号',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_match_apply_log_apply_id` (`apply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='牵线申请日志表';

CREATE TABLE IF NOT EXISTS `love_member_package` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员套餐编号',
  `name` varchar(64) NOT NULL COMMENT '套餐名称',
  `level` tinyint NOT NULL DEFAULT 0 COMMENT '会员等级',
  `price_fen` int NOT NULL DEFAULT 0 COMMENT '支付金额(分)',
  `duration_months` int NOT NULL DEFAULT 0 COMMENT '有效月数',
  `description` varchar(255) DEFAULT NULL COMMENT '套餐描述',
  `features_json` text DEFAULT NULL COMMENT '权益列表 JSON',
  `theme` varchar(32) NOT NULL DEFAULT 'silver' COMMENT '展示主题',
  `popular` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否推荐',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_member_package_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员套餐表';

CREATE TABLE IF NOT EXISTS `love_member_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员分组编号',
  `code` varchar(32) NOT NULL COMMENT '分组编码',
  `name` varchar(64) NOT NULL COMMENT '分组名称',
  `level` tinyint NOT NULL DEFAULT 0 COMMENT '会员等级',
  `theme` varchar(32) NOT NULL DEFAULT '' COMMENT '主题',
  `benefits_json` text DEFAULT NULL COMMENT '权益 JSON',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_love_member_group_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员分组表';

CREATE TABLE IF NOT EXISTS `love_member_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员 SKU 编号',
  `group_id` bigint NOT NULL COMMENT '会员分组编号',
  `duration_type` tinyint NOT NULL DEFAULT 0 COMMENT '时长类型',
  `duration_days` int NOT NULL DEFAULT 0 COMMENT '时长天数',
  `sale_price_fen` int NOT NULL DEFAULT 0 COMMENT '销售价(分)',
  `original_price_fen` int NOT NULL DEFAULT 0 COMMENT '原价(分)',
  `tag_text` varchar(64) DEFAULT NULL COMMENT '标签文案',
  `recommend` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否推荐',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_member_sku_group_id` (`group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员 SKU 表';

CREATE TABLE IF NOT EXISTS `love_member_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员订单编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `package_id` bigint NOT NULL COMMENT '套餐编号',
  `pay_order_id` bigint DEFAULT NULL COMMENT '支付订单编号',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `package_name` varchar(64) NOT NULL COMMENT '套餐名称快照',
  `member_level` tinyint NOT NULL DEFAULT 0 COMMENT '会员等级快照',
  `price_fen` int NOT NULL DEFAULT 0 COMMENT '支付金额(分)',
  `duration_months` int NOT NULL DEFAULT 0 COMMENT '有效月数',
  `group_id` bigint DEFAULT NULL COMMENT '会员分组编号',
  `sku_id` bigint DEFAULT NULL COMMENT '会员 SKU 编号',
  `group_code` varchar(32) DEFAULT NULL COMMENT '会员分组编码',
  `group_name` varchar(64) DEFAULT NULL COMMENT '会员分组名称',
  `duration_type` tinyint DEFAULT NULL COMMENT '时长类型',
  `duration_days` int DEFAULT NULL COMMENT '时长天数',
  `paid_amount_fen` int DEFAULT NULL COMMENT '实付金额(分)',
  `order_type` tinyint DEFAULT NULL COMMENT '订单类型',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `member_start_time` datetime DEFAULT NULL COMMENT '会员开始时间',
  `member_end_time` datetime DEFAULT NULL COMMENT '会员结束时间',
  `start_time` datetime DEFAULT NULL COMMENT '权益开始时间',
  `expire_time` datetime DEFAULT NULL COMMENT '权益结束时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_love_member_order_order_no` (`order_no`),
  KEY `idx_love_member_order_user_id` (`user_id`),
  KEY `idx_love_member_order_package_id` (`package_id`),
  KEY `idx_love_member_order_pay_order_id` (`pay_order_id`),
  KEY `idx_love_member_order_group_id` (`group_id`),
  KEY `idx_love_member_order_sku_id` (`sku_id`),
  KEY `idx_love_member_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员订单表';

CREATE TABLE IF NOT EXISTS `love_member_entitlement_segment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会员权益分段编号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `source_order_id` bigint NOT NULL COMMENT '来源订单编号',
  `source_order_type` tinyint NOT NULL DEFAULT 0 COMMENT '来源订单类型',
  `group_id` bigint NOT NULL COMMENT '会员分组编号',
  `sku_id` bigint NOT NULL COMMENT '会员 SKU 编号',
  `group_code` varchar(32) NOT NULL COMMENT '会员分组编码',
  `group_name` varchar(64) NOT NULL COMMENT '会员分组名称',
  `duration_type` tinyint NOT NULL DEFAULT 0 COMMENT '时长类型',
  `duration_days` int NOT NULL DEFAULT 0 COMMENT '时长天数',
  `paid_amount_fen` int NOT NULL DEFAULT 0 COMMENT '实付金额(分)',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `expire_time` datetime NOT NULL COMMENT '结束时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_love_member_segment_user_id` (`user_id`),
  KEY `idx_love_member_segment_group_id` (`group_id`),
  KEY `idx_love_member_segment_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员权益分段表';

CREATE TABLE IF NOT EXISTS `love_member_upgrade_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '升级订单编号',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `user_id` bigint NOT NULL COMMENT '用户编号',
  `from_group_id` bigint NOT NULL COMMENT '原会员分组编号',
  `to_group_id` bigint NOT NULL COMMENT '目标会员分组编号',
  `target_sku_id` bigint NOT NULL COMMENT '目标会员 SKU 编号',
  `remaining_days` int NOT NULL DEFAULT 0 COMMENT '剩余天数',
  `full_diff_amount_fen` int NOT NULL DEFAULT 0 COMMENT '完整差价(分)',
  `upgrade_amount_fen` int NOT NULL DEFAULT 0 COMMENT '补差价金额(分)',
  `pay_order_id` bigint DEFAULT NULL COMMENT '支付订单编号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_love_member_upgrade_order_no` (`order_no`),
  KEY `idx_love_member_upgrade_user_id` (`user_id`),
  KEY `idx_love_member_upgrade_pay_order_id` (`pay_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员升级订单表';

INSERT INTO `system_oauth2_client`
(`client_id`, `secret`, `name`, `logo`, `description`, `status`,
 `access_token_validity_seconds`, `refresh_token_validity_seconds`, `redirect_uris`,
 `authorized_grant_types`, `scopes`, `auto_approve_scopes`, `authorities`, `resource_ids`,
 `additional_information`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'love-mini-app', 'love-mini-app', '婚恋小程序', '', '婚恋业务小程序客户端', 0,
       1800, 2592000, '[]',
       '["password","refresh_token","authorization_code"]', '["love.read","love.write"]',
       '[]', '[]', '[]', '{}', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_oauth2_client` WHERE `client_id` = 'love-mini-app' AND `deleted` = b'0'
);

INSERT INTO `love_matchmaker`
(`name`, `avatar`, `introduction`, `mobile`, `wechat_no`, `sort`,
 `is_default`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '默认红娘', '', '系统默认分配红娘', NULL, NULL, 0, b'1', 0, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `love_matchmaker` WHERE `name` = '默认红娘' AND `deleted` = b'0'
);

INSERT INTO `love_member_package`
(`name`, `level`, `price_fen`, `duration_months`, `description`, `features_json`,
 `theme`, `popular`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '白银特权', 1, 99800, 3, '开启无限畅聊之旅',
       '["解锁查看所有人完整资料","每月推荐 10 位同城优质异性","无限次发送牵线申请"]',
       'silver', b'0', 10, 0, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `love_member_package` WHERE `name` = '白银特权' AND `duration_months` = 3 AND `deleted` = b'0'
);

INSERT INTO `love_member_package`
(`name`, `level`, `price_fen`, `duration_months`, `description`, `features_json`,
 `theme`, `popular`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '黄金管家', 2, 298000, 6, '专属红娘，深度跟进',
       '["包含所有白银特权","本地红娘一对一深度建档把关","精准定向匹配，优先推荐","协助安排线下见面与反馈"]',
       'gold', b'1', 20, 0, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `love_member_package` WHERE `name` = '黄金管家' AND `duration_months` = 6 AND `deleted` = b'0'
);

INSERT INTO `love_member_package`
(`name`, `level`, `price_fen`, `duration_months`, `description`, `features_json`,
 `theme`, `popular`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '黄金年卡', 2, 568000, 12, '全年专属红娘服务',
       '["包含所有黄金管家权益","全年深度跟进服务","高优先级定向推荐","线下见面持续复盘"]',
       'gold', b'0', 30, 0, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `love_member_package` WHERE `name` = '黄金年卡' AND `duration_months` = 12 AND `deleted` = b'0'
);

INSERT INTO `love_member_group`
(`code`, `name`, `level`, `theme`, `benefits_json`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'silver', '白银会员', 1, 'silver',
       '["基础身份认证","每日 30 次匹配","解锁查看访客","白银专属徽章"]',
       10, 0, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `love_member_group` WHERE `code` = 'silver' AND `deleted` = b'0'
);

INSERT INTO `love_member_group`
(`code`, `name`, `level`, `theme`, `benefits_json`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 'gold', '黄金会员', 2, 'gold',
       '["更高曝光率","优先推荐机会","专属客服支持","无限畅聊","尊贵身份标识"]',
       20, 0, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `love_member_group` WHERE `code` = 'gold' AND `deleted` = b'0'
);

INSERT INTO `love_member_sku`
(`group_id`, `duration_type`, `duration_days`, `sale_price_fen`, `original_price_fen`, `tag_text`, `recommend`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT g.id, 1, 90, 99800, 99800, '适合先体验', b'0', 10, 0, 'system', NOW(), 'system', NOW(), b'0'
FROM `love_member_group` g
WHERE g.code = 'silver'
  AND NOT EXISTS (SELECT 1 FROM `love_member_sku` s WHERE s.group_id = g.id AND s.duration_days = 90 AND s.deleted = b'0');

INSERT INTO `love_member_sku`
(`group_id`, `duration_type`, `duration_days`, `sale_price_fen`, `original_price_fen`, `tag_text`, `recommend`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT g.id, 2, 180, 179800, 199600, '立省 ¥198', b'0', 20, 0, 'system', NOW(), 'system', NOW(), b'0'
FROM `love_member_group` g
WHERE g.code = 'silver'
  AND NOT EXISTS (SELECT 1 FROM `love_member_sku` s WHERE s.group_id = g.id AND s.duration_days = 180 AND s.deleted = b'0');

INSERT INTO `love_member_sku`
(`group_id`, `duration_type`, `duration_days`, `sale_price_fen`, `original_price_fen`, `tag_text`, `recommend`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT g.id, 3, 365, 319800, 399200, '推荐', b'1', 30, 0, 'system', NOW(), 'system', NOW(), b'0'
FROM `love_member_group` g
WHERE g.code = 'silver'
  AND NOT EXISTS (SELECT 1 FROM `love_member_sku` s WHERE s.group_id = g.id AND s.duration_days = 365 AND s.deleted = b'0');

INSERT INTO `love_member_sku`
(`group_id`, `duration_type`, `duration_days`, `sale_price_fen`, `original_price_fen`, `tag_text`, `recommend`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT g.id, 1, 90, 169800, 169800, '适合短期体验', b'0', 40, 0, 'system', NOW(), 'system', NOW(), b'0'
FROM `love_member_group` g
WHERE g.code = 'gold'
  AND NOT EXISTS (SELECT 1 FROM `love_member_sku` s WHERE s.group_id = g.id AND s.duration_days = 90 AND s.deleted = b'0');

INSERT INTO `love_member_sku`
(`group_id`, `duration_type`, `duration_days`, `sale_price_fen`, `original_price_fen`, `tag_text`, `recommend`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT g.id, 2, 180, 299800, 339600, '立省 ¥398', b'0', 50, 0, 'system', NOW(), 'system', NOW(), b'0'
FROM `love_member_group` g
WHERE g.code = 'gold'
  AND NOT EXISTS (SELECT 1 FROM `love_member_sku` s WHERE s.group_id = g.id AND s.duration_days = 180 AND s.deleted = b'0');

INSERT INTO `love_member_sku`
(`group_id`, `duration_type`, `duration_days`, `sale_price_fen`, `original_price_fen`, `tag_text`, `recommend`, `sort`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT g.id, 3, 365, 539800, 679200, '最划算', b'1', 60, 0, 'system', NOW(), 'system', NOW(), b'0'
FROM `love_member_group` g
WHERE g.code = 'gold'
  AND NOT EXISTS (SELECT 1 FROM `love_member_sku` s WHERE s.group_id = g.id AND s.duration_days = 365 AND s.deleted = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '婚恋系统', '', 1, 210, 0, '/love', 'ep:star-filled', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` WHERE `path` = '/love' AND `type` = 1 AND `deleted` = b'0'
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员分组', 'love:member-group:query', 2, 10, parent.id, 'member-group', 'ep:medal', 'love/member-group/index', 'LoveMemberGroup', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`path` = '/love' AND parent.`type` = 1 AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `component` = 'love/member-group/index' AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员 SKU', 'love:member-sku:query', 2, 20, parent.id, 'member-sku', 'ep:collection-tag', 'love/member-sku/index', 'LoveMemberSku', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`path` = '/love' AND parent.`type` = 1 AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `component` = 'love/member-sku/index' AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员订单', 'love:member-order:query', 2, 30, parent.id, 'member-order', 'ep:tickets', 'love/member-order/index', 'LoveMemberOrder', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`path` = '/love' AND parent.`type` = 1 AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `component` = 'love/member-order/index' AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员升级订单', 'love:member-upgrade-order:query', 2, 40, parent.id, 'member-upgrade-order', 'ep:sort', 'love/member-upgrade-order/index', 'LoveMemberUpgradeOrder', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`path` = '/love' AND parent.`type` = 1 AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `component` = 'love/member-upgrade-order/index' AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员分组查询', 'love:member-group:query', 3, 1, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-group/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-group:query' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员分组创建', 'love:member-group:create', 3, 2, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-group/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-group:create' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员分组更新', 'love:member-group:update', 3, 3, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-group/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-group:update' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员分组删除', 'love:member-group:delete', 3, 4, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-group/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-group:delete' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员 SKU 查询', 'love:member-sku:query', 3, 1, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-sku/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-sku:query' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员 SKU 创建', 'love:member-sku:create', 3, 2, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-sku/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-sku:create' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员 SKU 更新', 'love:member-sku:update', 3, 3, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-sku/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-sku:update' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员 SKU 删除', 'love:member-sku:delete', 3, 4, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-sku/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-sku:delete' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员订单查询', 'love:member-order:query', 3, 1, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-order/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-order:query' AND `type` = 3 AND `deleted` = b'0');

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '会员升级订单查询', 'love:member-upgrade-order:query', 3, 1, parent.id, '', '', '', '', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`component` = 'love/member-upgrade-order/index' AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `permission` = 'love:member-upgrade-order:query' AND `type` = 3 AND `deleted` = b'0');

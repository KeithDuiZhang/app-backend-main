-- love-member-init.sql
-- 婚恋会员业务增量初始化脚本

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
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `member_start_time` datetime DEFAULT NULL COMMENT '会员开始时间',
  `member_end_time` datetime DEFAULT NULL COMMENT '会员结束时间',
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
  KEY `idx_love_member_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='婚恋会员订单表';

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

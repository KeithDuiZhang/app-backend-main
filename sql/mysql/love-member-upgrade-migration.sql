-- love-member-upgrade-migration.sql
-- 增量迁移：将旧版会员订单模型升级到「会员分组 + SKU + 补差价升级」模型

SET NAMES utf8mb4;

-- 1) love_member_order 增量补列
SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'group_id'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `group_id` bigint DEFAULT NULL COMMENT ''会员分组编号'' AFTER `duration_months`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'sku_id'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `sku_id` bigint DEFAULT NULL COMMENT ''会员 SKU 编号'' AFTER `group_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'group_code'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `group_code` varchar(32) DEFAULT NULL COMMENT ''会员分组编码'' AFTER `sku_id`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'group_name'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `group_name` varchar(64) DEFAULT NULL COMMENT ''会员分组名称'' AFTER `group_code`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'duration_type'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `duration_type` tinyint DEFAULT NULL COMMENT ''时长类型'' AFTER `group_name`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'duration_days'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `duration_days` int DEFAULT NULL COMMENT ''时长天数'' AFTER `duration_type`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'paid_amount_fen'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `paid_amount_fen` int DEFAULT NULL COMMENT ''实付金额(分)'' AFTER `duration_days`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'order_type'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `order_type` tinyint DEFAULT NULL COMMENT ''订单类型'' AFTER `paid_amount_fen`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'start_time'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `start_time` datetime DEFAULT NULL COMMENT ''权益开始时间'' AFTER `member_end_time`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND COLUMN_NAME = 'expire_time'
);
SET @sql := IF(
  @column_exists = 0,
  'ALTER TABLE `love_member_order` ADD COLUMN `expire_time` datetime DEFAULT NULL COMMENT ''权益结束时间'' AFTER `start_time`',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) love_member_order 增量补索引
SET @index_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND INDEX_NAME = 'idx_love_member_order_group_id'
);
SET @sql := IF(
  @index_exists = 0,
  'ALTER TABLE `love_member_order` ADD KEY `idx_love_member_order_group_id` (`group_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'love_member_order'
    AND INDEX_NAME = 'idx_love_member_order_sku_id'
);
SET @sql := IF(
  @index_exists = 0,
  'ALTER TABLE `love_member_order` ADD KEY `idx_love_member_order_sku_id` (`sku_id`)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 新表创建
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

-- 4) 会员分组和 SKU 种子
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

-- 5) 后台菜单和权限
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
SELECT '会员升级订单', 'love:member-upgrade-order:query', 2, 40, parent.id, 'member-upgrade-order', 'ep:sort', 'love/member-upgrade-order/index', 'LoveMemberUpgradeOrder', 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM `system_menu` parent
WHERE parent.`path` = '/love' AND parent.`type` = 1 AND parent.`deleted` = b'0'
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `component` = 'love/member-upgrade-order/index' AND `deleted` = b'0');

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

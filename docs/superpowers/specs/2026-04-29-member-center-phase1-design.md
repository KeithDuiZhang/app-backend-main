# 小程序会员中心第一版 1:1 复刻与后端动态化设计

## 1. 背景

当前婚恋小程序会员页已经具备基础购买能力，但仍采用“平铺套餐”模型：

- 页面直接展示多个会员套餐卡片
- 后端 `love_member_package` 仅支持单套餐平铺数据
- 会员升级链路尚未按“补差价升级”方式建模

本次目标是基于原型与《小程序会员套餐与补差价升级方案》，完成第一版会员中心重构：

- 小程序会员页按原型 1:1 复刻
- 新增小程序升级页，承接白银升级黄金
- 后端改为动态返回“会员类型 + SKU + 升级信息”
- 普通购买继续复用现有支付主链路
- 补差价升级补齐真实可运行的前链路

## 2. 目标

- 小程序会员页严格对齐原型信息结构、视觉层级、底部 CTA 和弹层交互
- 新增升级页，支持白银会员查看补差价明细并发起升级支付
- 后端新增会员组、SKU、权益分段、升级订单等支撑结构
- 小程序仅负责展示和提交动作，不自行计算补差价
- SVG 图标风格与原型保持一致，优先复用现有图标体系，不足时按现有命名规则补齐
- 第一版真实支持：
  - 白银 / 黄金两档会员
  - 季度 / 半年 / 年度三种时长
  - 白银升级黄金
  - 到期时间保持不变
  - 按实际支付金额和剩余有效期计算补差价

## 3. 非目标

- 本次不做会员体系全面重构，不直接移除现有 `love_member_package` 和 `love_member_order`
- 本次不支持黄金降级白银
- 本次不支持多级会员体系扩展到白金、钻石等更多档位的完整运营后台
- 本次不重做现有管理端页面的整体交互体验，只补齐最小可配置能力
- 本次不接入虚假的限时活动、倒计时、库存紧迫文案

## 4. 范围

本次实现范围覆盖两个仓库：

- `D:/CodeX/ruoyi-love-match/love-match-miniapp`
- `D:/CodeX/ruoyi-love-match/love-match-backend`

`love-match-frontend` 在本阶段不作为主实现目标，仅保留后续后台配置扩展的兼容空间。

## 5. 总体方案

采用“保留旧交易主链路 + 新增会员中心聚合层”的方案。

### 5.1 保留的旧能力

- 现有会员订单创建链路
- 现有支付订单创建与回调同步能力
- 现有 `love_user.memberLevel` 与 `memberExpireTime` 的快速态能力

### 5.2 新增的能力

- 会员组与 SKU 的规范化建模
- 面向小程序的会员中心聚合接口
- 面向升级页的补差价测算接口
- 升级订单创建接口
- 会员权益分段表，用于支持续费累加与升级精算

### 5.3 架构原则

- 小程序只做渲染和交互编排，不做业务金额计算
- 补差价金额统一由后端计算，金额单位统一用分
- 旧接口先不删除，新增 `member-center` 聚合接口承接新页面
- 普通购买由新聚合层桥接到旧支付链路
- 升级购买单独建模，避免把升级逻辑硬塞进普通会员订单

## 6. 小程序页面设计

### 6.1 页面结构

#### 会员首页

重做 `pages/member/index.vue`，页面结构固定为：

- 当前会员卡片
- 会员权益入口
- 会员类型切换区
- 时长 SKU 选择区
- 页面底部信任信息
- 固定底部 CTA
- 权益弹层
- 支付确认弹层

页面行为：

- 默认展示白银或根据当前会员高亮当前档位
- 默认选中年度 SKU
- 当用户切换到黄金且当前为有效白银会员时，显示可升级提示
- 当选中的黄金 SKU 命中升级条件时，底部 CTA 展示补差价升级
- 未登录用户点击购买或升级时先登录

#### 升级页

新增 `pages/member-upgrade/index.vue`，页面结构固定为：

- 当前状态卡片
- 升级后权益卡片
- 价格明细卡片
- 价格计算明细弹层
- 支付确认弹层
- 底部固定升级 CTA

页面行为：

- 页面从会员首页进入时带上目标 `groupId` 与 `durationType`
- 打开页时请求升级信息接口
- 所有明细均直接使用后端返回，不由前端推导

### 6.2 视觉还原策略

- 保留现有 `custom navigation` 与通用组件体系
- 按原型重做：
  - 当前会员卡片
  - 金银主题卡片
  - 推荐角标
  - 时长卡片选中态
  - 固定底部渐变遮罩与按钮区
  - 升级页价格卡与信任说明
- `AppButton`、`AppBottomSheet`、`AppIcon`、`AppTopNav` 继续复用
- 如通用组件样式不够，将做增量扩展，不推翻既有组件

### 6.3 图标策略

- 优先复用 `static/icons` 现有 SVG 资源
- 统一通过 `AppIcon.vue` 渲染
- 若原型图标在现有资源中不存在，则补齐到 `static/icons/<name>/ic-<name>-<color>.svg`
- 不使用临时 PNG 或异风格第三方图标替代原型关键图标

## 7. 后端数据设计

### 7.1 保留表

- `love_member_package`
- `love_member_order`
- `love_user`

这些表继续承担当前交易与用户快速态职责。

### 7.2 新增表

#### `member_group`

字段建议：

- `id`
- `name`
- `code`
- `level`
- `description`
- `benefits_json`
- `theme`
- `subtitle`
- `sort`
- `status`
- `created_at`
- `updated_at`

职责：

- 描述白银、黄金等会员档位
- 承载会员组展示文案、主题与权益摘要

#### `member_sku`

字段建议：

- `id`
- `group_id`
- `duration_type`
- `duration_days`
- `duration_months`
- `title`
- `description`
- `original_price_cents`
- `sale_price_cents`
- `tag`
- `ribbon_tag`
- `is_recommend`
- `sort`
- `status`
- `created_at`
- `updated_at`

职责：

- 描述季度、半年、年度等具体售卖规格
- 提供会员首页直接渲染所需的价格、标签与推荐信息

#### `member_entitlement_segment`

字段建议：

- `id`
- `user_id`
- `group_id`
- `sku_id`
- `source_order_id`
- `source_order_type`
- `duration_type`
- `start_time`
- `end_time`
- `paid_amount_cents`
- `status`
- `created_at`
- `updated_at`

职责：

- 记录用户每一段真实生效的会员权益
- 为续费累加和升级补差价计算提供精确依据

#### `member_upgrade_order`

字段建议：

- `id`
- `order_no`
- `user_id`
- `old_group_id`
- `new_group_id`
- `target_sku_id`
- `remaining_days`
- `full_diff_amount_cents`
- `upgrade_amount_cents`
- `status`
- `pay_order_id`
- `pay_time`
- `old_expire_time`
- `new_expire_time`
- `created_at`
- `updated_at`

职责：

- 独立承载补差价升级订单
- 与普通购买订单分离，避免后续逻辑复杂化

## 8. 后端服务与接口设计

### 8.1 聚合服务

新增会员中心聚合服务层，职责包括：

- 组装会员首页所需结构
- 组装升级页所需结构
- 统一判定当前用户是否可升级
- 统一计算补差价金额
- 将购买动作桥接到现有支付订单逻辑

### 8.2 接口清单

#### `GET /love/member-center/packages`

用途：

- 会员首页完整数据接口

返回结构：

- `currentMember`
- `groupTabs`
- `groups`
- `pageTips`

其中：

- `currentMember` 包含 `groupCode`、`groupName`、`level`、`expireTime`、`remainingDays`、`canUpgradeToGold`
- `groupTabs` 供顶部切换使用
- `groups[].skus[]` 供时长选择区直接渲染
- `estimatedUpgradePriceFen` 仅在满足升级条件时返回

#### `GET /love/member-center/upgrade-info`

用途：

- 升级页完整数据接口

建议参数：

- `targetGroupId`
- `durationType`

返回结构：

- `currentMember`
- `targetMember`
- `priceDetail`
- `detailLines`
- `tips`

#### `POST /love/member-center/order/create`

用途：

- 普通购买入口

请求参数：

- `skuId`

行为：

- 校验 SKU 是否存在且启用
- 映射到可支付交易对象
- 创建普通支付单
- 返回 `payOrderId` 及页面支付所需信息

#### `POST /love/member-center/upgrade-order/create`

用途：

- 补差价升级入口

请求参数：

- `targetGroupId`
- `durationType`

行为：

- 校验当前是否允许升级
- 重新实时计算补差价
- 创建升级订单
- 创建支付单
- 返回 `payOrderId` 及支付所需信息

## 9. 补差价规则

### 9.1 第一版支持范围

- 仅支持 `白银 -> 黄金`
- 仅支持当前存在有效会员时升级
- 到期时间保持不变
- 黄金权益支付成功后立即生效

### 9.2 单段公式

对于单段有效会员：

`upgradeAmountFen = ceil((targetPriceFen - paidAmountFen) * remainingSeconds / totalSeconds)`

规则：

- 若剩余时间小于等于 0，则不可升级
- 若差价小于 0，则按 0 处理
- 若计算结果低于最低支付金额，则判定不可升级

### 9.3 多段公式

对于多段有效会员：

- 遍历所有未过期 segment
- 按每段的实际支付金额、目标同周期 SKU、该段剩余有效期分别计算
- 求和后向上取整

### 9.4 金额口径

- 所有金额统一存储和计算为分
- 所有页面展示金额在后端可同时返回展示值或由前端格式化
- 补差价必须使用“实际支付金额”，不能使用原价

## 10. 业务状态与异常处理

### 10.1 会员首页

- 未登录：可浏览，不可支付
- 非会员：可直接购买
- 有效白银：可续费白银，可升级黄金
- 有效黄金：不可升级，只可购买或续费黄金
- 已过期白银：不可升级，提示直接购买黄金

### 10.2 升级校验

以下场景后端直接拒绝：

- 当前无有效会员
- 当前已是黄金会员
- 目标会员等级不高于当前等级
- 找不到目标周期 SKU
- 剩余有效期太短导致金额低于最小支付金额

### 10.3 支付与幂等

- 普通购买与升级支付均需做回调幂等
- 已成功订单不可重复生效
- 已关闭支付单不可重新激活会员

## 11. 后台配置边界

第一版不直接大改现有 `love/member-package` 后台页面为复杂的双层结构，而是遵循“最小侵入”原则：

- 现有旧套餐管理保留，避免影响当前交易链路
- 新增会员组与 SKU 的管理能力
- 若时间不允许完整做新后台页面，则第一版至少提供初始化 SQL 与基础管理接口

后台配置至少要支持：

- 会员组名称、编码、等级、主题、状态、排序
- SKU 时长、原价、售价、标签、推荐标记、状态、排序
- 权益文案列表
- 首页和升级页提示文案

## 12. 数据流

### 12.1 普通购买

- 小程序请求 `packages`
- 用户选择会员类型与 SKU
- 小程序调用 `order/create`
- 后端桥接旧支付订单链路
- 用户支付
- 支付回调成功
- 写入或追加 `member_entitlement_segment`
- 更新 `love_user.memberLevel` 与 `memberExpireTime`

### 12.2 补差价升级

- 小程序会员首页获取 `packages`
- 命中升级条件时进入升级页
- 升级页请求 `upgrade-info`
- 用户确认后调用 `upgrade-order/create`
- 用户支付补差价
- 支付回调成功
- 更新所有未过期 segment 的 `group_id`
- 更新 `love_user.memberLevel`
- 保持 `memberExpireTime` 不变

## 13. 文件级改动方向

### 13.1 小程序

重点文件：

- `pages/member/index.vue`
- `pages/member-upgrade/index.vue`
- `components/AppButton.vue`
- `components/AppBottomSheet.vue`
- `components/AppIcon.vue`
- `api/member-package.js`
- `api/member-order.js`
- 新增 `api/member-center.js`
- `pages.json`
- `static/icons/**`

### 13.2 后端

重点目录：

- `controller/app/membercenter/**`
- `controller/app/memberorder/**`
- `service/membercenter/**`
- `service/memberpackage/**`
- `service/memberorder/**`
- `dal/dataobject/member/**`
- `dal/mysql/member/**`
- 初始化 SQL / migration 文件

## 14. 测试策略

### 14.1 后端

- 聚合接口单元测试
- 升级金额计算测试
- 正常购买下单测试
- 升级下单测试
- 白银升级黄金、黄金升级黄金、过期白银升级等异常场景测试

### 14.2 小程序

- 会员首页 UI 对照原型检查
- 白银 / 黄金切换
- 年度默认选中
- 底部 CTA 在不同会员状态下切换正确
- 权益弹层、价格明细弹层、支付弹层展示正确
- 图标资源不缺失

### 14.3 联调验收

验收主路径：

- 非会员购买白银年度
- 有效白银用户查看黄金年度升级页
- 有效白银用户发起升级支付
- 有效黄金用户查看会员页无升级入口
- 过期白银用户进入黄金页仅显示直接购买

## 15. 风险与权衡

- 现有旧套餐模型仍会与新模型并存一段时间，需要桥接映射
- 若不引入 `member_entitlement_segment`，后续升级精算和续费追溯会迅速复杂化，因此本次即使第一版也建议落表
- 若后台新配置页不在本轮完成，可先用 SQL 初始化和管理接口保证功能落地，再补后台表单

## 16. 最终结论

第一版采用“旧交易主链路保留，新会员中心聚合层新增”的方式最稳妥：

- 小程序能按原型 1:1 复刻
- 后端能动态返回会员组、SKU 和升级数据
- 普通购买风险最低
- 白银升级黄金可以真实落地
- 后续再扩展后台配置、多段升级和更多会员档位时，不需要推倒重来

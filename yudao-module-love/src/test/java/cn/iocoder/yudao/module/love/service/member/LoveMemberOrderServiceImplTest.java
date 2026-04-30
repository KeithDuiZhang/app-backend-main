package cn.iocoder.yudao.module.love.service.member;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberEntitlementSegmentDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberOrderDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberEntitlementSegmentMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberOrderMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.service.memberpackage.LoveMemberPackageService;
import cn.iocoder.yudao.module.love.service.memberorder.LoveMemberOrderServiceImpl;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(LoveMemberOrderServiceImpl.class)
@TestPropertySource(properties = {
        "yudao.info.base-package=cn.iocoder.yudao.module.love",
        "spring.sql.init.mode=never"
})
public class LoveMemberOrderServiceImplTest extends BaseDbUnitTest {

    private static final int STATUS_CREATED = 0;
    private static final int STATUS_ACTIVE = 10;
    private static final int ORDER_TYPE_PURCHASE = 1;

    @Resource
    private LoveMemberOrderServiceImpl loveMemberOrderService;
    @Resource
    private LoveMemberOrderMapper loveMemberOrderMapper;
    @Resource
    private LoveMemberEntitlementSegmentMapper loveMemberEntitlementSegmentMapper;
    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private DataSource dataSource;

    @MockitoBean
    private PayOrderApi payOrderApi;
    @MockitoBean
    private LoveMemberPackageService loveMemberPackageService;

    @BeforeEach
    public void setUpSchema() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_member_entitlement_segment");
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_member_order");
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_member_sku");
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_member_group");
        jdbcTemplate.execute("DROP TABLE IF EXISTS love_user");

        jdbcTemplate.execute("""
                CREATE TABLE love_user (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  social_user_id BIGINT DEFAULT NULL,
                  openid VARCHAR(64) DEFAULT NULL,
                  unionid VARCHAR(64) DEFAULT NULL,
                  nickname VARCHAR(64) DEFAULT NULL,
                  avatar VARCHAR(255) DEFAULT NULL,
                  mobile VARCHAR(20) DEFAULT NULL,
                  status TINYINT NOT NULL DEFAULT 0,
                  auth_status TINYINT NOT NULL DEFAULT 0,
                  certified_at TIMESTAMP DEFAULT NULL,
                  member_level TINYINT NOT NULL DEFAULT 0,
                  member_expire_time TIMESTAMP DEFAULT NULL,
                  free_match_quota INT NOT NULL DEFAULT 0,
                  last_quota_reset_at TIMESTAMP DEFAULT NULL,
                  last_login_time TIMESTAMP DEFAULT NULL,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE love_member_group (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  code VARCHAR(32) NOT NULL,
                  name VARCHAR(64) NOT NULL,
                  level TINYINT NOT NULL DEFAULT 0,
                  theme VARCHAR(32) NOT NULL DEFAULT '',
                  benefits_json CLOB DEFAULT NULL,
                  sort INT NOT NULL DEFAULT 0,
                  status TINYINT NOT NULL DEFAULT 0,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE love_member_sku (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  group_id BIGINT NOT NULL,
                  sku_code VARCHAR(32) NOT NULL,
                  sku_name VARCHAR(64) NOT NULL,
                  duration_type TINYINT NOT NULL DEFAULT 1,
                  duration_days INT NOT NULL DEFAULT 0,
                  line_price_fen INT NOT NULL DEFAULT 0,
                  sale_price_fen INT NOT NULL DEFAULT 0,
                  tag_text VARCHAR(32) DEFAULT NULL,
                  recommend BOOLEAN NOT NULL DEFAULT FALSE,
                  sort INT NOT NULL DEFAULT 0,
                  status TINYINT NOT NULL DEFAULT 0,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE love_member_order (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  pay_order_id BIGINT DEFAULT NULL,
                  order_no VARCHAR(64) NOT NULL,
                  package_id BIGINT DEFAULT NULL,
                  package_name VARCHAR(64) DEFAULT NULL,
                  member_level TINYINT NOT NULL DEFAULT 0,
                  price_fen INT NOT NULL DEFAULT 0,
                  duration_months INT NOT NULL DEFAULT 0,
                  group_id BIGINT NOT NULL,
                  sku_id BIGINT NOT NULL,
                  group_code VARCHAR(32) NOT NULL,
                  group_name VARCHAR(64) NOT NULL,
                  duration_type TINYINT NOT NULL DEFAULT 1,
                  duration_days INT NOT NULL DEFAULT 0,
                  paid_amount_fen INT NOT NULL DEFAULT 0,
                  order_type TINYINT NOT NULL DEFAULT 0,
                  status TINYINT NOT NULL DEFAULT 0,
                  pay_time TIMESTAMP DEFAULT NULL,
                  member_start_time TIMESTAMP DEFAULT NULL,
                  member_end_time TIMESTAMP DEFAULT NULL,
                  start_time TIMESTAMP DEFAULT NULL,
                  expire_time TIMESTAMP DEFAULT NULL,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE love_member_entitlement_segment (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                  user_id BIGINT NOT NULL,
                  source_order_id BIGINT NOT NULL,
                  source_order_type TINYINT NOT NULL DEFAULT 0,
                  group_id BIGINT NOT NULL,
                  sku_id BIGINT NOT NULL,
                  group_code VARCHAR(32) NOT NULL,
                  group_name VARCHAR(64) NOT NULL,
                  duration_type TINYINT NOT NULL DEFAULT 1,
                  duration_days INT NOT NULL DEFAULT 0,
                  paid_amount_fen INT NOT NULL DEFAULT 0,
                  start_time TIMESTAMP NOT NULL,
                  expire_time TIMESTAMP NOT NULL,
                  creator VARCHAR(64) DEFAULT '',
                  create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  updater VARCHAR(64) DEFAULT '',
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  deleted TINYINT NOT NULL DEFAULT 0
                )
                """);
    }

    @Test
    public void testActivatePaidMemberOrder_renewsFromCurrentExpiryAndCreatesSegment() {
        LocalDateTime currentExpireTime = LocalDateTime.of(2026, 5, 20, 9, 30);
        LocalDateTime payTime = LocalDateTime.of(2026, 5, 1, 8, 0);
        LoveUserDO user = LoveUserDO.builder()
                .memberLevel(1)
                .memberExpireTime(currentExpireTime)
                .build();
        loveUserMapper.insert(user);
        LoveMemberOrderDO order = LoveMemberOrderDO.builder()
                .userId(user.getId())
                .orderNo("MEM202605010001")
                .packageId(2L)
                .packageName("白银半年卡")
                .memberLevel(1)
                .priceFen(179800)
                .durationMonths(6)
                .groupId(1L)
                .skuId(2L)
                .groupCode("silver")
                .groupName("白银会员")
                .durationType(1)
                .durationDays(180)
                .paidAmountFen(179800)
                .orderType(ORDER_TYPE_PURCHASE)
                .status(STATUS_CREATED)
                .build();
        loveMemberOrderMapper.insert(order);

        loveMemberOrderService.activatePaidMemberOrder(order.getId(), payTime);

        LoveMemberOrderDO refreshedOrder = loveMemberOrderMapper.selectById(order.getId());
        assertEquals(STATUS_ACTIVE, refreshedOrder.getStatus());
        assertEquals(currentExpireTime, refreshedOrder.getMemberStartTime());
        assertEquals(currentExpireTime.plusDays(180), refreshedOrder.getMemberEndTime());
        assertEquals(payTime, refreshedOrder.getPayTime());

        LoveUserDO refreshedUser = loveUserMapper.selectById(user.getId());
        assertEquals(1, refreshedUser.getMemberLevel());
        assertEquals(currentExpireTime.plusDays(180), refreshedUser.getMemberExpireTime());

        List<LoveMemberEntitlementSegmentDO> segments = loveMemberEntitlementSegmentMapper.selectList();
        assertEquals(1, segments.size());
        LoveMemberEntitlementSegmentDO segment = segments.get(0);
        assertNotNull(segment.getId());
        assertEquals(order.getId(), segment.getSourceOrderId());
        assertEquals(ORDER_TYPE_PURCHASE, segment.getSourceOrderType());
        assertEquals(1L, segment.getGroupId());
        assertEquals(2L, segment.getSkuId());
        assertEquals(179800, segment.getPaidAmountFen());
        assertEquals(currentExpireTime, segment.getStartTime());
        assertEquals(currentExpireTime.plusDays(180), segment.getExpireTime());
    }
}

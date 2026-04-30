package cn.iocoder.yudao.module.love.service.member;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberEntitlementSegmentDO;
import cn.iocoder.yudao.module.love.service.memberupgrade.LoveMemberUpgradeServiceImpl;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(LoveMemberUpgradeServiceImpl.class)
@TestPropertySource(properties = {
        "yudao.info.base-package=cn.iocoder.yudao.module.love",
        "spring.sql.init.mode=never"
})
public class LoveMemberUpgradeServiceImplTest extends BaseDbUnitTest {

    @Resource
    private LoveMemberUpgradeServiceImpl loveMemberUpgradeService;

    @MockitoBean
    private PayOrderApi payOrderApi;

    @Test
    public void testCalculateUpgradePayAmountFen_proratesRemainingEntitlement() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0);
        LoveMemberEntitlementSegmentDO segment = LoveMemberEntitlementSegmentDO.builder()
                .groupCode("WHITE_SILVER")
                .groupName("白银会员")
                .durationDays(365)
                .paidAmountFen(319800)
                .startTime(now.minusDays(185))
                .expireTime(now.plusDays(180))
                .build();

        Integer amountFen = loveMemberUpgradeService.calculateUpgradePayAmountFen(
                539800, List.of(segment), now);

        assertEquals(108493, amountFen);
    }

    @Test
    public void testCalculateUpgradePayAmountFen_skipsExpiredSegment() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0);
        LoveMemberEntitlementSegmentDO expiredSegment = LoveMemberEntitlementSegmentDO.builder()
                .durationDays(365)
                .paidAmountFen(319800)
                .startTime(now.minusDays(365))
                .expireTime(now.minusMinutes(1))
                .build();

        Integer amountFen = loveMemberUpgradeService.calculateUpgradePayAmountFen(
                539800, List.of(expiredSegment), now);

        assertEquals(0, amountFen);
    }

    @Test
    public void testCalculateUpgradePayAmountFen_negativeDiffClampsToZero() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0);
        LoveMemberEntitlementSegmentDO segment = LoveMemberEntitlementSegmentDO.builder()
                .durationDays(365)
                .paidAmountFen(539800)
                .startTime(now.minusDays(185))
                .expireTime(now.plusDays(180))
                .build();

        Integer amountFen = loveMemberUpgradeService.calculateUpgradePayAmountFen(
                319800, List.of(segment), now);

        assertEquals(0, amountFen);
    }

    @Test
    public void testCalculateUpgradePayAmountFen_multipleSegmentsSum() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0);
        LoveMemberEntitlementSegmentDO segmentOne = LoveMemberEntitlementSegmentDO.builder()
                .durationDays(365)
                .paidAmountFen(319800)
                .startTime(now.minusDays(185))
                .expireTime(now.plusDays(180))
                .build();
        LoveMemberEntitlementSegmentDO segmentTwo = LoveMemberEntitlementSegmentDO.builder()
                .durationDays(30)
                .paidAmountFen(10000)
                .startTime(now.minusDays(10))
                .expireTime(now.plusDays(20))
                .build();

        Integer amountFen = loveMemberUpgradeService.calculateUpgradePayAmountFen(
                539800, List.of(segmentOne, segmentTwo), now);

        assertEquals(461693, amountFen);
    }

    @Test
    public void testCalculateUpgradePayAmountFen_usesFullTimePrecision() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 29, 12, 0);
        LoveMemberEntitlementSegmentDO segment = LoveMemberEntitlementSegmentDO.builder()
                .durationDays(1)
                .paidAmountFen(100)
                .startTime(now.minusHours(12))
                .expireTime(now.plusHours(12))
                .build();

        Integer amountFen = loveMemberUpgradeService.calculateUpgradePayAmountFen(
                200, List.of(segment), now);

        assertEquals(50, amountFen);
    }
}

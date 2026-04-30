package cn.iocoder.yudao.module.love.service.memberupgrade;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradePreviewRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo.AppLoveMemberUpgradeResultRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberEntitlementSegmentDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberGroupDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberSkuDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberUpgradeOrderDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberEntitlementSegmentMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberGroupMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberSkuMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberUpgradeOrderMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_GROUP_NOT_EXISTS;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_SKU_NOT_EXISTS;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_UPGRADE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_UPGRADE_ORDER_NOT_EXISTS;

@Service
public class LoveMemberUpgradeServiceImpl implements LoveMemberUpgradeService {

    private static final int STATUS_CREATED = 0;
    private static final int STATUS_PAYING = 5;

    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveMemberGroupMapper loveMemberGroupMapper;
    @Resource
    private LoveMemberSkuMapper loveMemberSkuMapper;
    @Resource
    private LoveMemberEntitlementSegmentMapper loveMemberEntitlementSegmentMapper;
    @Resource
    private LoveMemberUpgradeOrderMapper loveMemberUpgradeOrderMapper;
    @Resource
    private PayOrderApi payOrderApi;

    @Override
    public Integer calculateUpgradePayAmountFen(Integer targetSalePriceFen,
                                                List<LoveMemberEntitlementSegmentDO> entitlementSegments,
                                                LocalDateTime calculateAt) {
        if (targetSalePriceFen == null || targetSalePriceFen <= 0 || entitlementSegments == null || entitlementSegments.isEmpty()) {
            return 0;
        }

        long totalAmountFen = 0L;
        for (LoveMemberEntitlementSegmentDO segment : entitlementSegments) {
            if (segment == null || segment.getExpireTime() == null || !segment.getExpireTime().isAfter(calculateAt)) {
                continue;
            }
            long totalNanos = resolveTotalNanos(segment);
            if (totalNanos <= 0) {
                continue;
            }

            long priceDiffFen = (long) targetSalePriceFen - defaultValue(segment.getPaidAmountFen());
            if (priceDiffFen <= 0) {
                continue;
            }

            long remainingNanos = Duration.between(calculateAt, segment.getExpireTime()).toNanos();
            if (remainingNanos <= 0) {
                continue;
            }
            remainingNanos = Math.min(remainingNanos, totalNanos);

            totalAmountFen += BigInteger.valueOf(priceDiffFen)
                    .multiply(BigInteger.valueOf(remainingNanos))
                    .divide(BigInteger.valueOf(totalNanos))
                    .longValueExact();
        }
        return Math.toIntExact(totalAmountFen);
    }

    @Override
    public AppLoveMemberUpgradePreviewRespVO preview(Long userId, Long targetSkuId) {
        LoveMemberSkuDO targetSku = getSku(targetSkuId);
        LoveMemberGroupDO targetGroup = getGroup(targetSku.getGroupId());
        LocalDateTime now = LocalDateTime.now();
        List<LoveMemberEntitlementSegmentDO> segments = getActiveSegments(userId, now);
        LoveUserDO user = loveUserMapper.selectById(userId);
        LoveMemberEntitlementSegmentDO latestSegment = getLatestSegment(segments);

        AppLoveMemberUpgradePreviewRespVO respVO = new AppLoveMemberUpgradePreviewRespVO();
        respVO.setUpgradeState(resolveUpgradeState(user, targetGroup, segments));
        respVO.setCanUpgrade("upgradeable".equals(respVO.getUpgradeState()));
        respVO.setCurrentMember(buildCurrentMember(user, latestSegment, now));
        respVO.setTargetMember(buildTargetMember(targetGroup, targetSku));
        respVO.setTips(List.of("升级后黄金权益立即生效", "会员到期时间保持不变", "补差价按实际支付金额和剩余有效期计算"));

        int currentPaidAmountFen = resolveCurrentPaidFen(latestSegment);
        int fullDiffAmountFen = targetSku.getSalePriceFen() - currentPaidAmountFen;
        int upgradeAmountFen = calculateUpgradePayAmountFen(targetSku.getSalePriceFen(), segments, now);
        AppLoveMemberUpgradePreviewRespVO.PriceDetail price = new AppLoveMemberUpgradePreviewRespVO.PriceDetail();
        price.setTargetSalePriceFen(targetSku.getSalePriceFen());
        price.setCurrentPaidAmountFen(currentPaidAmountFen);
        price.setFullDiffAmountFen(Math.max(fullDiffAmountFen, 0));
        price.setRemainingDays(respVO.getCurrentMember() != null ? respVO.getCurrentMember().getRemainingDays() : 0);
        price.setTotalDurationDays(resolveTotalDurationDays(latestSegment));
        price.setUpgradeAmountFen(upgradeAmountFen);
        respVO.setPrice(price);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoveMemberUpgradeCreateRespVO createUpgradeOrder(Long userId, Long targetSkuId, String userIp) {
        AppLoveMemberUpgradePreviewRespVO preview = preview(userId, targetSkuId);
        if (!Boolean.TRUE.equals(preview.getCanUpgrade())) {
            throw exception(LOVE_MEMBER_UPGRADE_NOT_ALLOWED);
        }
        LoveMemberSkuDO targetSku = getSku(targetSkuId);
        LoveMemberGroupDO targetGroup = getGroup(targetSku.getGroupId());
        LoveUserDO user = loveUserMapper.selectById(userId);

        LoveMemberUpgradeOrderDO order = LoveMemberUpgradeOrderDO.builder()
                .orderNo(IdUtil.fastSimpleUUID())
                .userId(userId)
                .fromGroupId(resolveCurrentGroupId(userId))
                .toGroupId(targetGroup.getId())
                .targetSkuId(targetSkuId)
                .remainingDays(preview.getCurrentMember() != null ? preview.getCurrentMember().getRemainingDays() : 0)
                .fullDiffAmountFen(preview.getPrice().getFullDiffAmountFen())
                .upgradeAmountFen(preview.getPrice().getUpgradeAmountFen())
                .status(STATUS_CREATED)
                .build();
        loveMemberUpgradeOrderMapper.insert(order);

        PayOrderCreateReqDTO payOrderCreateReqDTO = new PayOrderCreateReqDTO();
        payOrderCreateReqDTO.setAppKey(LoveBizConstants.APP_KEY);
        payOrderCreateReqDTO.setUserIp(userIp);
        payOrderCreateReqDTO.setUserId(userId);
        payOrderCreateReqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        payOrderCreateReqDTO.setMerchantOrderId(order.getOrderNo());
        payOrderCreateReqDTO.setSubject(targetGroup.getName() + "补差价升级");
        payOrderCreateReqDTO.setBody("婚恋会员补差价升级");
        payOrderCreateReqDTO.setPrice(order.getUpgradeAmountFen());
        payOrderCreateReqDTO.setExpireTime(LocalDateTime.now().plusMinutes(30));
        Long payOrderId = payOrderApi.createOrder(payOrderCreateReqDTO);

        order.setPayOrderId(payOrderId);
        order.setStatus(STATUS_PAYING);
        loveMemberUpgradeOrderMapper.updateById(order);

        AppLoveMemberUpgradeCreateRespVO respVO = new AppLoveMemberUpgradeCreateRespVO();
        respVO.setBizOrderId(order.getId());
        respVO.setPayOrderId(payOrderId);
        respVO.setPayAmountFen(order.getUpgradeAmountFen());
        respVO.setTargetGroupCode(targetGroup.getCode());
        return respVO;
    }

    @Override
    public AppLoveMemberUpgradeResultRespVO getUpgradeResult(Long userId, Long orderId) {
        LoveMemberUpgradeOrderDO order = loveMemberUpgradeOrderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw exception(LOVE_MEMBER_UPGRADE_ORDER_NOT_EXISTS);
        }
        LoveUserDO user = loveUserMapper.selectById(userId);
        LoveMemberGroupDO fromGroup = loveMemberGroupMapper.selectById(order.getFromGroupId());
        LoveMemberGroupDO toGroup = loveMemberGroupMapper.selectById(order.getToGroupId());
        AppLoveMemberUpgradeResultRespVO respVO = new AppLoveMemberUpgradeResultRespVO();
        respVO.setId(order.getId());
        respVO.setFromGroupCode(fromGroup != null ? fromGroup.getCode() : null);
        respVO.setToGroupCode(toGroup != null ? toGroup.getCode() : null);
        respVO.setToGroupName(toGroup != null ? toGroup.getName() : null);
        respVO.setUpgradeAmountFen(order.getUpgradeAmountFen());
        respVO.setExpireTime(user != null ? user.getMemberExpireTime() : null);
        respVO.setPayTime(order.getPayTime());
        return respVO;
    }

    private static long resolveTotalNanos(LoveMemberEntitlementSegmentDO segment) {
        if (segment.getStartTime() != null && segment.getExpireTime() != null
                && segment.getExpireTime().isAfter(segment.getStartTime())) {
            return Duration.between(segment.getStartTime(), segment.getExpireTime()).toNanos();
        }
        Integer durationDays = segment.getDurationDays();
        if (durationDays == null || durationDays <= 0) {
            return 0;
        }
        return Duration.ofDays(durationDays.longValue()).toNanos();
    }

    private static int defaultValue(Integer value) {
        return value == null ? 0 : value;
    }

    private LoveMemberSkuDO getSku(Long targetSkuId) {
        LoveMemberSkuDO sku = loveMemberSkuMapper.selectById(targetSkuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 0) {
            throw exception(LOVE_MEMBER_SKU_NOT_EXISTS);
        }
        return sku;
    }

    private LoveMemberGroupDO getGroup(Long groupId) {
        LoveMemberGroupDO group = loveMemberGroupMapper.selectById(groupId);
        if (group == null || group.getStatus() == null || group.getStatus() != 0) {
            throw exception(LOVE_MEMBER_GROUP_NOT_EXISTS);
        }
        return group;
    }

    private List<LoveMemberEntitlementSegmentDO> getActiveSegments(Long userId, LocalDateTime now) {
        return loveMemberEntitlementSegmentMapper.selectList(new LambdaQueryWrapperX<LoveMemberEntitlementSegmentDO>()
                .eq(LoveMemberEntitlementSegmentDO::getUserId, userId)
                .ge(LoveMemberEntitlementSegmentDO::getExpireTime, now)
                .orderByDesc(LoveMemberEntitlementSegmentDO::getExpireTime));
    }

    private String resolveUpgradeState(LoveUserDO user, LoveMemberGroupDO targetGroup, List<LoveMemberEntitlementSegmentDO> segments) {
        if (user == null || user.getMemberLevel() == null || user.getMemberLevel() <= 0 || segments.isEmpty()) {
            return "expired";
        }
        if (user.getMemberLevel() >= targetGroup.getLevel()) {
            return "already_gold";
        }
        return "upgradeable";
    }

    private AppLoveMemberUpgradePreviewRespVO.CurrentMember buildCurrentMember(LoveUserDO user,
                                                                               LoveMemberEntitlementSegmentDO latestSegment,
                                                                               LocalDateTime now) {
        if (user == null || latestSegment == null) {
            return null;
        }
        AppLoveMemberUpgradePreviewRespVO.CurrentMember currentMember = new AppLoveMemberUpgradePreviewRespVO.CurrentMember();
        currentMember.setGroupId(latestSegment.getGroupId());
        currentMember.setGroupCode(latestSegment.getGroupCode());
        currentMember.setGroupName(latestSegment.getGroupName());
        currentMember.setMemberLevel(user.getMemberLevel());
        currentMember.setDurationType(latestSegment.getDurationType());
        currentMember.setDurationDays(latestSegment.getDurationDays());
        currentMember.setExpireTime(user.getMemberExpireTime());
        currentMember.setRemainingDays((int) Math.max(Duration.between(now, latestSegment.getExpireTime()).toDays(), 0));
        return currentMember;
    }

    private AppLoveMemberUpgradePreviewRespVO.TargetMember buildTargetMember(LoveMemberGroupDO targetGroup, LoveMemberSkuDO targetSku) {
        AppLoveMemberUpgradePreviewRespVO.TargetMember targetMember = new AppLoveMemberUpgradePreviewRespVO.TargetMember();
        targetMember.setGroupId(targetGroup.getId());
        targetMember.setGroupCode(targetGroup.getCode());
        targetMember.setGroupName(targetGroup.getName());
        targetMember.setTargetSkuId(targetSku.getId());
        targetMember.setDurationType(targetSku.getDurationType());
        targetMember.setDurationDays(targetSku.getDurationDays());
        return targetMember;
    }

    private LoveMemberEntitlementSegmentDO getLatestSegment(List<LoveMemberEntitlementSegmentDO> segments) {
        return segments.stream()
                .max(Comparator.comparing(LoveMemberEntitlementSegmentDO::getExpireTime))
                .orElse(null);
    }

    private int resolveCurrentPaidFen(LoveMemberEntitlementSegmentDO latestSegment) {
        return latestSegment != null ? defaultValue(latestSegment.getPaidAmountFen()) : 0;
    }

    private int resolveTotalDurationDays(LoveMemberEntitlementSegmentDO latestSegment) {
        if (latestSegment == null) {
            return 0;
        }
        if (latestSegment.getDurationDays() != null && latestSegment.getDurationDays() > 0) {
            return latestSegment.getDurationDays();
        }
        if (latestSegment.getStartTime() != null && latestSegment.getExpireTime() != null
                && latestSegment.getExpireTime().isAfter(latestSegment.getStartTime())) {
            return Math.toIntExact(Math.max(Duration.between(latestSegment.getStartTime(), latestSegment.getExpireTime()).toDays(), 0));
        }
        return 0;
    }

    private Long resolveCurrentGroupId(Long userId) {
        return loveMemberEntitlementSegmentMapper.selectList(new LambdaQueryWrapperX<LoveMemberEntitlementSegmentDO>()
                        .eq(LoveMemberEntitlementSegmentDO::getUserId, userId)
                        .orderByDesc(LoveMemberEntitlementSegmentDO::getExpireTime)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(LoveMemberEntitlementSegmentDO::getGroupId)
                .orElse(null);
    }
}

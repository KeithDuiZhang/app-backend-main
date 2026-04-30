package cn.iocoder.yudao.module.love.service.memberorder;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.module.love.controller.admin.memberorder.vo.LoveMemberOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderResultRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberEntitlementSegmentDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberGroupDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberOrderDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberSkuDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberEntitlementSegmentMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberGroupMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberOrderMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberSkuMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_ORDER_NOT_EXISTS;

@Service
public class LoveMemberOrderServiceImpl implements LoveMemberOrderService {

    private static final int STATUS_CREATED = 0;
    private static final int STATUS_PAYING = 5;
    private static final int STATUS_ACTIVE = 10;
    private static final int STATUS_CLOSED = 20;

    @Resource
    private LoveMemberOrderMapper loveMemberOrderMapper;
    @Resource
    private LoveMemberEntitlementSegmentMapper loveMemberEntitlementSegmentMapper;
    @Resource
    private LoveMemberSkuMapper loveMemberSkuMapper;
    @Resource
    private LoveMemberGroupMapper loveMemberGroupMapper;
    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private PayOrderApi payOrderApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoveMemberOrderCreateRespVO createMemberOrder(Long userId, Long packageId, String userIp) {
        LoveMemberSkuDO sku = loveMemberSkuMapper.selectById(packageId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 0) {
            throw exception(LOVE_MEMBER_ORDER_NOT_EXISTS);
        }
        LoveMemberGroupDO group = loveMemberGroupMapper.selectById(sku.getGroupId());
        if (group == null || group.getStatus() == null || group.getStatus() != 0) {
            throw exception(LOVE_MEMBER_ORDER_NOT_EXISTS);
        }

        LoveMemberOrderDO memberOrder = LoveMemberOrderDO.builder()
                .userId(userId)
                .packageId(sku.getId())
                .orderNo(IdUtil.fastSimpleUUID())
                .packageName(buildPackageName(group, sku))
                .memberLevel(group.getLevel())
                .priceFen(sku.getSalePriceFen())
                .durationMonths(resolveDurationMonths(sku.getDurationDays()))
                .groupId(group.getId())
                .skuId(sku.getId())
                .groupCode(group.getCode())
                .groupName(group.getName())
                .durationType(sku.getDurationType())
                .durationDays(sku.getDurationDays())
                .paidAmountFen(sku.getSalePriceFen())
                .orderType(1)
                .status(STATUS_CREATED)
                .build();
        loveMemberOrderMapper.insert(memberOrder);

        PayOrderCreateReqDTO payOrderCreateReqDTO = new PayOrderCreateReqDTO();
        payOrderCreateReqDTO.setAppKey(LoveBizConstants.APP_KEY);
        payOrderCreateReqDTO.setUserIp(userIp);
        payOrderCreateReqDTO.setUserId(userId);
        payOrderCreateReqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        payOrderCreateReqDTO.setMerchantOrderId(memberOrder.getOrderNo());
        payOrderCreateReqDTO.setSubject(memberOrder.getPackageName());
        payOrderCreateReqDTO.setBody("婚恋会员服务费");
        payOrderCreateReqDTO.setPrice(memberOrder.getPaidAmountFen());
        payOrderCreateReqDTO.setExpireTime(LocalDateTime.now().plusMinutes(30));
        Long payOrderId = payOrderApi.createOrder(payOrderCreateReqDTO);

        memberOrder.setPayOrderId(payOrderId);
        memberOrder.setStatus(STATUS_PAYING);
        loveMemberOrderMapper.updateById(memberOrder);

        AppLoveMemberOrderCreateRespVO respVO = new AppLoveMemberOrderCreateRespVO();
        respVO.setBizOrderId(memberOrder.getId());
        respVO.setPayOrderId(payOrderId);
        respVO.setPackageId(memberOrder.getPackageId());
        respVO.setPackageName(memberOrder.getPackageName());
        respVO.setPriceFen(memberOrder.getPriceFen());
        respVO.setStatus(memberOrder.getStatus());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoveMemberOrderRespVO getCurrentOrder(Long userId) {
        LoveMemberOrderDO memberOrder = getLatestOrder(userId);
        if (memberOrder == null) {
            return null;
        }
        syncAndActivate(memberOrder);
        return convertAppResp(memberOrder);
    }

    @Override
    public AppLoveMemberOrderResultRespVO getOrderResult(Long userId, Long orderId) {
        LoveMemberOrderDO memberOrder = getMemberOrder(orderId);
        if (!memberOrder.getUserId().equals(userId)) {
            throw exception(LOVE_MEMBER_ORDER_NOT_EXISTS);
        }
        syncAndActivate(memberOrder);
        AppLoveMemberOrderResultRespVO respVO = new AppLoveMemberOrderResultRespVO();
        respVO.setId(memberOrder.getId());
        respVO.setSkuId(memberOrder.getSkuId());
        respVO.setGroupCode(memberOrder.getGroupCode());
        respVO.setGroupName(memberOrder.getGroupName());
        respVO.setPackageName(memberOrder.getPackageName());
        respVO.setMemberLevel(memberOrder.getMemberLevel());
        respVO.setPaidAmountFen(memberOrder.getPaidAmountFen() != null ? memberOrder.getPaidAmountFen() : memberOrder.getPriceFen());
        respVO.setPayTime(memberOrder.getPayTime());
        respVO.setStartTime(memberOrder.getStartTime() != null ? memberOrder.getStartTime() : memberOrder.getMemberStartTime());
        respVO.setExpireTime(memberOrder.getExpireTime() != null ? memberOrder.getExpireTime() : memberOrder.getMemberEndTime());
        return respVO;
    }

    @Override
    public PageResult<LoveMemberOrderDO> getMemberOrderPage(LoveMemberOrderPageReqVO reqVO) {
        return loveMemberOrderMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMemberOrderDO>()
                .eqIfPresent(LoveMemberOrderDO::getUserId, reqVO.getUserId())
                .eqIfPresent(LoveMemberOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(LoveMemberOrderDO::getMemberLevel, reqVO.getMemberLevel())
                .likeIfPresent(LoveMemberOrderDO::getOrderNo, reqVO.getOrderNo())
                .likeIfPresent(LoveMemberOrderDO::getPackageName, reqVO.getPackageName())
                .orderByDesc(LoveMemberOrderDO::getId));
    }

    @Override
    public LoveMemberOrderDO getMemberOrder(Long id) {
        LoveMemberOrderDO memberOrder = loveMemberOrderMapper.selectById(id);
        if (memberOrder == null) {
            throw exception(LOVE_MEMBER_ORDER_NOT_EXISTS);
        }
        return memberOrder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activatePaidMemberOrder(Long orderId, LocalDateTime payTime) {
        LoveMemberOrderDO memberOrder = getMemberOrder(orderId);
        activateMemberOrder(memberOrder, payTime);
    }

    private LoveMemberOrderDO getLatestOrder(Long userId) {
        return loveMemberOrderMapper.selectOne(new LambdaQueryWrapperX<LoveMemberOrderDO>()
                .eq(LoveMemberOrderDO::getUserId, userId)
                .orderByDesc(LoveMemberOrderDO::getId)
                .last("LIMIT 1"));
    }

    private void syncAndActivate(LoveMemberOrderDO memberOrder) {
        if (memberOrder.getPayOrderId() == null) {
            return;
        }
        PayOrderRespDTO payOrder = payOrderApi.getOrder(memberOrder.getPayOrderId());
        if (payOrder != null && PayOrderStatusEnum.isWaiting(payOrder.getStatus())) {
            payOrderApi.syncOrderQuietly(memberOrder.getPayOrderId());
            payOrder = payOrderApi.getOrder(memberOrder.getPayOrderId());
        }
        if (payOrder != null && PayOrderStatusEnum.isSuccess(payOrder.getStatus())) {
            activateMemberOrder(memberOrder, payOrder.getSuccessTime());
        } else if (payOrder != null && PayOrderStatusEnum.isClosed(payOrder.getStatus()) && memberOrder.getStatus() != STATUS_CLOSED) {
            memberOrder.setStatus(STATUS_CLOSED);
            loveMemberOrderMapper.updateById(memberOrder);
        }
    }

    private void activateMemberOrder(LoveMemberOrderDO memberOrder, LocalDateTime payTime) {
        if (memberOrder.getStatus() == STATUS_ACTIVE && memberOrder.getMemberEndTime() != null) {
            return;
        }
        LoveUserDO user = loveUserMapper.selectById(memberOrder.getUserId());
        if (user == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime memberStartTime = user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(now)
                ? user.getMemberExpireTime() : now;
        int durationDays = resolveDurationDays(memberOrder);
        LocalDateTime memberEndTime = memberStartTime.plusDays(durationDays);

        memberOrder.setStatus(STATUS_ACTIVE);
        memberOrder.setPayTime(payTime);
        memberOrder.setMemberStartTime(memberStartTime);
        memberOrder.setMemberEndTime(memberEndTime);
        memberOrder.setStartTime(memberStartTime);
        memberOrder.setExpireTime(memberEndTime);
        loveMemberOrderMapper.updateById(memberOrder);

        user.setMemberLevel(memberOrder.getMemberLevel());
        user.setMemberExpireTime(memberEndTime);
        loveUserMapper.updateById(user);

        if (memberOrder.getGroupId() != null && memberOrder.getSkuId() != null) {
            loveMemberEntitlementSegmentMapper.insert(buildEntitlementSegment(memberOrder, memberStartTime, memberEndTime));
        }
    }

    private int resolveDurationDays(LoveMemberOrderDO memberOrder) {
        if (memberOrder.getDurationDays() != null && memberOrder.getDurationDays() > 0) {
            return memberOrder.getDurationDays();
        }
        if (memberOrder.getDurationMonths() != null && memberOrder.getDurationMonths() > 0) {
            return memberOrder.getDurationMonths() * 30;
        }
        throw new IllegalStateException("会员订单缺少有效时长");
    }

    private int resolveDurationMonths(Integer durationDays) {
        if (durationDays == null) {
            return 0;
        }
        return Map.of(90, 3, 180, 6, 365, 12).getOrDefault(durationDays, Math.max(durationDays / 30, 0));
    }

    private String buildPackageName(LoveMemberGroupDO group, LoveMemberSkuDO sku) {
        return group.getName() + switch (resolveDurationMonths(sku.getDurationDays())) {
            case 3 -> "季度会员";
            case 6 -> "半年会员";
            case 12 -> "年度会员";
            default -> "会员";
        };
    }

    private LoveMemberEntitlementSegmentDO buildEntitlementSegment(LoveMemberOrderDO memberOrder,
                                                                   LocalDateTime memberStartTime,
                                                                   LocalDateTime memberEndTime) {
        return LoveMemberEntitlementSegmentDO.builder()
                .userId(memberOrder.getUserId())
                .sourceOrderId(memberOrder.getId())
                .sourceOrderType(memberOrder.getOrderType())
                .groupId(memberOrder.getGroupId())
                .skuId(memberOrder.getSkuId())
                .groupCode(memberOrder.getGroupCode())
                .groupName(memberOrder.getGroupName())
                .durationType(memberOrder.getDurationType())
                .durationDays(resolveDurationDays(memberOrder))
                .paidAmountFen(memberOrder.getPaidAmountFen() != null ? memberOrder.getPaidAmountFen() : memberOrder.getPriceFen())
                .startTime(memberStartTime)
                .expireTime(memberEndTime)
                .build();
    }

    private AppLoveMemberOrderRespVO convertAppResp(LoveMemberOrderDO memberOrder) {
        AppLoveMemberOrderRespVO respVO = new AppLoveMemberOrderRespVO();
        respVO.setId(memberOrder.getId());
        respVO.setUserId(memberOrder.getUserId());
        respVO.setPackageId(memberOrder.getPackageId());
        respVO.setPayOrderId(memberOrder.getPayOrderId());
        respVO.setOrderNo(memberOrder.getOrderNo());
        respVO.setPackageName(memberOrder.getPackageName());
        respVO.setMemberLevel(memberOrder.getMemberLevel());
        respVO.setPriceFen(memberOrder.getPriceFen());
        respVO.setDurationMonths(memberOrder.getDurationMonths());
        respVO.setStatus(memberOrder.getStatus());
        respVO.setPayTime(memberOrder.getPayTime());
        respVO.setMemberStartTime(memberOrder.getMemberStartTime());
        respVO.setMemberEndTime(memberOrder.getMemberEndTime());
        return respVO;
    }
}

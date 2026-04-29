package cn.iocoder.yudao.module.love.service.memberorder;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.module.love.controller.admin.memberorder.vo.LoveMemberOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.memberorder.vo.AppLoveMemberOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberOrderDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberPackageDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberOrderMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.service.memberpackage.LoveMemberPackageService;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    private LoveMemberPackageService loveMemberPackageService;
    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private PayOrderApi payOrderApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoveMemberOrderCreateRespVO createMemberOrder(Long userId, Long packageId, String userIp) {
        LoveMemberPackageDO memberPackage = loveMemberPackageService.validateMemberPackage(packageId);

        LoveMemberOrderDO memberOrder = LoveMemberOrderDO.builder()
                .userId(userId)
                .packageId(memberPackage.getId())
                .orderNo(IdUtil.fastSimpleUUID())
                .packageName(memberPackage.getName())
                .memberLevel(memberPackage.getLevel())
                .priceFen(memberPackage.getPriceFen())
                .durationMonths(memberPackage.getDurationMonths())
                .status(STATUS_CREATED)
                .build();
        loveMemberOrderMapper.insert(memberOrder);

        PayOrderCreateReqDTO payOrderCreateReqDTO = new PayOrderCreateReqDTO();
        payOrderCreateReqDTO.setAppKey(LoveBizConstants.APP_KEY);
        payOrderCreateReqDTO.setUserIp(userIp);
        payOrderCreateReqDTO.setUserId(userId);
        payOrderCreateReqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        payOrderCreateReqDTO.setMerchantOrderId(memberOrder.getOrderNo());
        payOrderCreateReqDTO.setSubject(memberPackage.getName());
        payOrderCreateReqDTO.setBody("婚恋会员服务费");
        payOrderCreateReqDTO.setPrice(memberPackage.getPriceFen());
        payOrderCreateReqDTO.setExpireTime(LocalDateTime.now().plusMinutes(30));
        Long payOrderId = payOrderApi.createOrder(payOrderCreateReqDTO);

        memberOrder.setPayOrderId(payOrderId);
        memberOrder.setStatus(STATUS_PAYING);
        loveMemberOrderMapper.updateById(memberOrder);

        AppLoveMemberOrderCreateRespVO respVO = new AppLoveMemberOrderCreateRespVO();
        respVO.setBizOrderId(memberOrder.getId());
        respVO.setPayOrderId(payOrderId);
        respVO.setPackageId(memberPackage.getId());
        respVO.setPackageName(memberPackage.getName());
        respVO.setPriceFen(memberPackage.getPriceFen());
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
        LocalDateTime memberEndTime = memberStartTime.plusMonths(memberOrder.getDurationMonths());

        memberOrder.setStatus(STATUS_ACTIVE);
        memberOrder.setPayTime(payTime);
        memberOrder.setMemberStartTime(memberStartTime);
        memberOrder.setMemberEndTime(memberEndTime);
        loveMemberOrderMapper.updateById(memberOrder);

        user.setMemberLevel(memberOrder.getMemberLevel());
        user.setMemberExpireTime(memberEndTime);
        loveUserMapper.updateById(user);
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

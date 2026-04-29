package cn.iocoder.yudao.module.love.service.authorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.module.love.controller.admin.authorder.vo.LoveAuthOrderPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.auth.LoveAuthOrderDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.mysql.auth.LoveAuthOrderMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.pay.api.order.PayOrderApi;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderCreateReqDTO;
import cn.iocoder.yudao.module.pay.api.order.dto.PayOrderRespDTO;
import cn.iocoder.yudao.module.pay.enums.order.PayOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@Validated
public class LoveAuthOrderServiceImpl implements LoveAuthOrderService {

    private static final int STATUS_CREATED = 0;
    private static final int STATUS_PAYING = 5;
    private static final int STATUS_VERIFIED = 10;

    @Resource
    private LoveAuthOrderMapper loveAuthOrderMapper;
    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private PayOrderApi payOrderApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoveAuthOrderCreateRespVO createAuthOrder(Long userId, String userIp) {
        LoveAuthOrderDO authOrder = LoveAuthOrderDO.builder()
                .userId(userId)
                .orderNo(IdUtil.fastSimpleUUID())
                .amount(LoveBizConstants.AUTH_FEE_FEN)
                .status(STATUS_CREATED)
                .build();
        loveAuthOrderMapper.insert(authOrder);

        PayOrderCreateReqDTO payOrderCreateReqDTO = new PayOrderCreateReqDTO();
        payOrderCreateReqDTO.setAppKey(LoveBizConstants.APP_KEY);
        payOrderCreateReqDTO.setUserIp(userIp);
        payOrderCreateReqDTO.setUserId(userId);
        payOrderCreateReqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        payOrderCreateReqDTO.setMerchantOrderId(authOrder.getOrderNo());
        payOrderCreateReqDTO.setSubject("实名认证");
        payOrderCreateReqDTO.setBody("婚恋平台实名认证服务费");
        payOrderCreateReqDTO.setPrice(LoveBizConstants.AUTH_FEE_FEN);
        payOrderCreateReqDTO.setExpireTime(LocalDateTime.now().plusMinutes(30));
        Long payOrderId = payOrderApi.createOrder(payOrderCreateReqDTO);

        authOrder.setPayOrderId(payOrderId);
        authOrder.setStatus(STATUS_PAYING);
        loveAuthOrderMapper.updateById(authOrder);

        AppLoveAuthOrderCreateRespVO respVO = new AppLoveAuthOrderCreateRespVO();
        respVO.setBizOrderId(authOrder.getId());
        respVO.setPayOrderId(payOrderId);
        respVO.setAmount(authOrder.getAmount());
        respVO.setStatus(authOrder.getStatus());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppLoveAuthOrderRespVO getCurrentOrder(Long userId) {
        LoveAuthOrderDO authOrder = loveAuthOrderMapper.selectLatestByUserId(userId);
        if (authOrder == null) {
            return null;
        }
        if (authOrder.getPayOrderId() != null) {
            PayOrderRespDTO payOrder = payOrderApi.getOrder(authOrder.getPayOrderId());
            if (payOrder != null && PayOrderStatusEnum.isWaiting(payOrder.getStatus())) {
                payOrderApi.syncOrderQuietly(authOrder.getPayOrderId());
                payOrder = payOrderApi.getOrder(authOrder.getPayOrderId());
            }
            if (payOrder != null && PayOrderStatusEnum.isSuccess(payOrder.getStatus())) {
                authOrder.setStatus(STATUS_VERIFIED);
                authOrder.setVerifiedResult("SUCCESS");
                authOrder.setPayTime(payOrder.getSuccessTime());
                authOrder.setVerifiedTime(LocalDateTime.now());
                loveAuthOrderMapper.updateById(authOrder);

                LoveUserDO user = loveUserMapper.selectById(userId);
                if (user != null) {
                    user.setAuthStatus(1);
                    user.setCertifiedAt(authOrder.getVerifiedTime());
                    loveUserMapper.updateById(user);
                }
            }
        }

        AppLoveAuthOrderRespVO respVO = new AppLoveAuthOrderRespVO();
        respVO.setId(authOrder.getId());
        respVO.setUserId(authOrder.getUserId());
        respVO.setPayOrderId(authOrder.getPayOrderId());
        respVO.setOrderNo(authOrder.getOrderNo());
        respVO.setAmount(authOrder.getAmount());
        respVO.setStatus(authOrder.getStatus());
        respVO.setVerifiedResult(authOrder.getVerifiedResult());
        respVO.setPayTime(authOrder.getPayTime());
        respVO.setVerifiedTime(authOrder.getVerifiedTime());
        return respVO;
    }

    @Override
    public PageResult<LoveAuthOrderDO> getAuthOrderPage(LoveAuthOrderPageReqVO reqVO) {
        return loveAuthOrderMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveAuthOrderDO>()
                .eqIfPresent(LoveAuthOrderDO::getUserId, reqVO.getUserId())
                .eqIfPresent(LoveAuthOrderDO::getStatus, reqVO.getStatus())
                .likeIfPresent(LoveAuthOrderDO::getOrderNo, reqVO.getOrderNo())
                .orderByDesc(LoveAuthOrderDO::getId));
    }

    @Override
    public LoveAuthOrderDO getAuthOrder(Long id) {
        return loveAuthOrderMapper.selectById(id);
    }
}

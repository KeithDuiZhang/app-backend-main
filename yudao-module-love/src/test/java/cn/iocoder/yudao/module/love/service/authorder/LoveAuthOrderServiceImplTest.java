package cn.iocoder.yudao.module.love.service.authorder;

import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
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
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoveAuthOrderServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private LoveAuthOrderServiceImpl loveAuthOrderService;

    @Mock
    private LoveAuthOrderMapper loveAuthOrderMapper;
    @Mock
    private LoveUserMapper loveUserMapper;
    @Mock
    private PayOrderApi payOrderApi;

    @Test
    public void testCreateAuthOrder() {
        doAnswer(invocation -> {
            LoveAuthOrderDO authOrder = invocation.getArgument(0);
            authOrder.setId(1L);
            return 1;
        }).when(loveAuthOrderMapper).insert(any(LoveAuthOrderDO.class));
        when(payOrderApi.createOrder(argThat(this::matchCreateReq))).thenReturn(100L);

        AppLoveAuthOrderCreateRespVO respVO = loveAuthOrderService.createAuthOrder(12L, "127.0.0.1");

        assertEquals(1L, respVO.getBizOrderId());
        assertEquals(100L, respVO.getPayOrderId());
        assertEquals(LoveBizConstants.AUTH_FEE_FEN, respVO.getAmount());
        assertEquals(5, respVO.getStatus());
        verify(loveAuthOrderMapper).updateById(any(LoveAuthOrderDO.class));
    }

    @Test
    public void testGetCurrentOrder_syncPaidOrderMarksVerified() {
        LoveAuthOrderDO authOrder = LoveAuthOrderDO.builder()
                .id(1L)
                .userId(12L)
                .payOrderId(100L)
                .orderNo("AUTH001")
                .amount(LoveBizConstants.AUTH_FEE_FEN)
                .status(5)
                .build();
        when(loveAuthOrderMapper.selectLatestByUserId(12L)).thenReturn(authOrder);
        PayOrderRespDTO payOrder = new PayOrderRespDTO();
        payOrder.setId(100L);
        payOrder.setStatus(PayOrderStatusEnum.SUCCESS.getStatus());
        payOrder.setSuccessTime(LocalDateTime.now());
        when(payOrderApi.getOrder(100L)).thenReturn(payOrder);
        LoveUserDO user = LoveUserDO.builder().id(12L).authStatus(0).build();
        when(loveUserMapper.selectById(12L)).thenReturn(user);

        AppLoveAuthOrderRespVO respVO = loveAuthOrderService.getCurrentOrder(12L);

        assertNotNull(respVO);
        assertEquals(10, respVO.getStatus());
        assertEquals("SUCCESS", respVO.getVerifiedResult());
        assertNotNull(respVO.getPayTime());
        assertNotNull(respVO.getVerifiedTime());
        assertEquals(1, user.getAuthStatus());
        assertNotNull(user.getCertifiedAt());
        verify(loveAuthOrderMapper).updateById(authOrder);
        verify(loveUserMapper).updateById(user);
    }

    @Test
    public void testGetCurrentOrderReturnsNullWhenAbsent() {
        when(loveAuthOrderMapper.selectLatestByUserId(99L)).thenReturn(null);

        AppLoveAuthOrderRespVO respVO = loveAuthOrderService.getCurrentOrder(99L);

        assertNull(respVO);
    }

    private boolean matchCreateReq(PayOrderCreateReqDTO reqDTO) {
        return reqDTO != null
                && LoveBizConstants.APP_KEY.equals(reqDTO.getAppKey())
                && "127.0.0.1".equals(reqDTO.getUserIp())
                && Long.valueOf(12L).equals(reqDTO.getUserId())
                && UserTypeEnum.MEMBER.getValue().equals(reqDTO.getUserType())
                && LoveBizConstants.AUTH_FEE_FEN.equals(reqDTO.getPrice())
                && "实名认证".equals(reqDTO.getSubject())
                && "婚恋平台实名认证服务费".equals(reqDTO.getBody())
                && reqDTO.getExpireTime() != null;
    }
}

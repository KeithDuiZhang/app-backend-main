package cn.iocoder.yudao.module.love.controller.app.authorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderCreateRespVO;
import cn.iocoder.yudao.module.love.controller.app.authorder.vo.AppLoveAuthOrderRespVO;
import cn.iocoder.yudao.module.love.service.authorder.LoveAuthOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class AppLoveAuthOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppLoveAuthOrderController appLoveAuthOrderController;

    @Mock
    private LoveAuthOrderService loveAuthOrderService;

    @Test
    public void testCreateAuthOrder() {
        AppLoveAuthOrderCreateRespVO respVO = new AppLoveAuthOrderCreateRespVO();
        respVO.setBizOrderId(1L);
        respVO.setPayOrderId(100L);
        respVO.setAmount(1990);
        respVO.setStatus(5);
        when(loveAuthOrderService.createAuthOrder(null, null)).thenReturn(respVO);

        CommonResult<AppLoveAuthOrderCreateRespVO> result = appLoveAuthOrderController.createAuthOrder();

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getBizOrderId());
        assertEquals(100L, result.getData().getPayOrderId());
    }

    @Test
    public void testGetCurrentOrder() {
        AppLoveAuthOrderRespVO respVO = new AppLoveAuthOrderRespVO();
        respVO.setId(2L);
        respVO.setStatus(10);
        when(loveAuthOrderService.getCurrentOrder(null)).thenReturn(respVO);

        CommonResult<AppLoveAuthOrderRespVO> result = appLoveAuthOrderController.getCurrentOrder();

        assertEquals(0, result.getCode());
        assertEquals(2L, result.getData().getId());
        assertEquals(10, result.getData().getStatus());
    }
}

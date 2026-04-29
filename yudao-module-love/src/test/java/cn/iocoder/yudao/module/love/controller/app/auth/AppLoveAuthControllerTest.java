package cn.iocoder.yudao.module.love.controller.app.auth;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.auth.vo.AppLoveWechatMiniLoginReqVO;
import cn.iocoder.yudao.module.love.controller.app.auth.vo.AppLoveWechatMiniLoginRespVO;
import cn.iocoder.yudao.module.love.service.auth.LoveAuthService;
import cn.iocoder.yudao.module.love.service.auth.dto.LoveAuthLoginRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppLoveAuthControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppLoveAuthController appLoveAuthController;

    @Mock
    private LoveAuthService loveAuthService;

    @Test
    public void testWechatMiniLogin() {
        AppLoveWechatMiniLoginReqVO reqVO = new AppLoveWechatMiniLoginReqVO();
        reqVO.setCode("mini-login-code");
        LoveAuthLoginRespDTO loginRespDTO = new LoveAuthLoginRespDTO()
                .setAccessToken("access-token")
                .setRefreshToken("refresh-token")
                .setUserId(123L)
                .setOpenid("openid-123")
                .setProfileCompleted(true)
                .setAuthStatus(2);
        when(loveAuthService.loginByWechatMiniCode(eq(reqVO.getCode()))).thenReturn(loginRespDTO);

        CommonResult<AppLoveWechatMiniLoginRespVO> result = appLoveAuthController.wechatMiniLogin(reqVO);

        assertEquals(0, result.getCode());
        assertEquals("access-token", result.getData().getAccessToken());
        assertEquals("refresh-token", result.getData().getRefreshToken());
        assertEquals(123L, result.getData().getUserId());
        assertEquals("openid-123", result.getData().getOpenid());
        assertTrue(result.getData().getProfileCompleted());
        assertEquals(2, result.getData().getAuthStatus());
        verify(loveAuthService).loginByWechatMiniCode(reqVO.getCode());
    }

}

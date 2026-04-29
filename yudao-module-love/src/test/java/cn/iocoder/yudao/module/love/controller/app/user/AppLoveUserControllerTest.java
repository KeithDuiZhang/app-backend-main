package cn.iocoder.yudao.module.love.controller.app.user;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.user.vo.AppLoveUserRespVO;
import cn.iocoder.yudao.module.love.service.user.LoveUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class AppLoveUserControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppLoveUserController appLoveUserController;

    @Mock
    private LoveUserService loveUserService;

    @Test
    public void testGetMy() {
        AppLoveUserRespVO respVO = new AppLoveUserRespVO();
        respVO.setId(1L);
        respVO.setNickname("测试用户");
        respVO.setFreeMatchQuota(1);
        when(loveUserService.getMyUser(null)).thenReturn(respVO);

        CommonResult<AppLoveUserRespVO> result = appLoveUserController.getMy();

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getId());
        assertEquals("测试用户", result.getData().getNickname());
    }
}

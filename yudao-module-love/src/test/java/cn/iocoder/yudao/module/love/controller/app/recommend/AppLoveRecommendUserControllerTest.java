package cn.iocoder.yudao.module.love.controller.app.recommend;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserRespVO;
import cn.iocoder.yudao.module.love.service.recommend.LoveRecommendUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class AppLoveRecommendUserControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppLoveRecommendUserController appLoveRecommendUserController;

    @Mock
    private LoveRecommendUserService loveRecommendUserService;

    @Test
    public void testGetRecommendPage() {
        AppLoveRecommendUserRespVO user = new AppLoveRecommendUserRespVO();
        user.setUserId(1L);
        user.setNickname("推荐用户");
        when(loveRecommendUserService.getRecommendPage(org.mockito.ArgumentMatchers.any(AppLoveRecommendUserPageReqVO.class)))
                .thenReturn(new PageResult<>(List.of(user), 1L));

        CommonResult<PageResult<AppLoveRecommendUserRespVO>> result =
                appLoveRecommendUserController.getRecommendPage(new AppLoveRecommendUserPageReqVO());

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals("推荐用户", result.getData().getList().get(0).getNickname());
    }

    @Test
    public void testGetRecommendUser() {
        AppLoveRecommendUserRespVO user = new AppLoveRecommendUserRespVO();
        user.setUserId(2L);
        user.setNickname("详情用户");
        when(loveRecommendUserService.getRecommendUser(2L)).thenReturn(user);

        CommonResult<AppLoveRecommendUserRespVO> result = appLoveRecommendUserController.getRecommendUser(2L);

        assertEquals(0, result.getCode());
        assertEquals(2L, result.getData().getUserId());
        assertEquals("详情用户", result.getData().getNickname());
    }
}

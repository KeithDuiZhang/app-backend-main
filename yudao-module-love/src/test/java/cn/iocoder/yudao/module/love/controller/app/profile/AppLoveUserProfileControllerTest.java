package cn.iocoder.yudao.module.love.controller.app.profile;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileRespVO;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileUpdateBasicReqVO;
import cn.iocoder.yudao.module.love.service.profile.LoveUserProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppLoveUserProfileControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AppLoveUserProfileController appLoveUserProfileController;

    @Mock
    private LoveUserProfileService loveUserProfileService;

    @Test
    public void testGetMyProfile() {
        AppLoveUserProfileRespVO respVO = new AppLoveUserProfileRespVO();
        respVO.setUserId(1L);
        respVO.setCityName("平江县");
        when(loveUserProfileService.getProfile(null)).thenReturn(respVO);

        CommonResult<AppLoveUserProfileRespVO> result = appLoveUserProfileController.getMyProfile();

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getUserId());
        assertEquals("平江县", result.getData().getCityName());
    }

    @Test
    public void testUpdateBasic() {
        AppLoveUserProfileUpdateBasicReqVO reqVO = new AppLoveUserProfileUpdateBasicReqVO();
        reqVO.setGender(1);
        reqVO.setBirthday(LocalDate.of(1995, 5, 1));
        reqVO.setCityCode("430626");
        reqVO.setCityName("平江县");
        reqVO.setMaritalStatus(1);

        CommonResult<Boolean> result = appLoveUserProfileController.updateBasic(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(loveUserProfileService).updateBasicProfile(null, reqVO);
    }
}

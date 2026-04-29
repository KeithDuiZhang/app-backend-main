package cn.iocoder.yudao.module.love.service.recommend;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoveRecommendUserServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private LoveRecommendUserServiceImpl loveRecommendUserService;

    @Mock
    private LoveUserMapper loveUserMapper;
    @Mock
    private LoveUserProfileMapper loveUserProfileMapper;

    @Test
    public void testGetRecommendPage() {
        LoveUserProfileDO profile = LoveUserProfileDO.builder()
                .userId(1L)
                .gender(2)
                .birthday(LocalDate.of(1998, 5, 1))
                .cityName("平江县")
                .maritalStatus(1)
                .bio("介绍")
                .tags("[\"同城\"]")
                .profilePublic(Boolean.TRUE)
                .build();
        when(loveUserProfileMapper.selectPage(any(AppLoveRecommendUserPageReqVO.class), any()))
                .thenReturn(new PageResult<>(List.of(profile), 1L));
        LoveUserDO user = LoveUserDO.builder()
                .id(1L)
                .nickname("推荐对象")
                .avatar("https://avatar")
                .authStatus(1)
                .memberLevel(0)
                .build();
        when(loveUserMapper.selectList(LoveUserDO::getId, List.of(1L))).thenReturn(List.of(user));

        AppLoveRecommendUserPageReqVO reqVO = new AppLoveRecommendUserPageReqVO();
        reqVO.setMinAge(20);
        reqVO.setMaxAge(35);

        PageResult<AppLoveRecommendUserRespVO> result = loveRecommendUserService.getRecommendPage(reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("推荐对象", result.getList().get(0).getNickname());
        verify(loveUserProfileMapper).selectPage(any(AppLoveRecommendUserPageReqVO.class), any());
    }
}

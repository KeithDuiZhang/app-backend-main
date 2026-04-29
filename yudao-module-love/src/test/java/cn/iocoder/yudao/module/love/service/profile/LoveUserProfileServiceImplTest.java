package cn.iocoder.yudao.module.love.service.profile;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.profile.vo.AppLoveUserProfileUpdateBasicReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoveUserProfileServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private LoveUserProfileServiceImpl loveUserProfileService;

    @Mock
    private LoveUserProfileMapper loveUserProfileMapper;

    @Test
    public void testUpdateBasicProfile() {
        LoveUserProfileDO profile = LoveUserProfileDO.builder()
                .id(10L)
                .userId(1L)
                .profilePublic(Boolean.TRUE)
                .completionRate(0)
                .build();
        when(loveUserProfileMapper.selectOne(anySFunction(), eq(1L))).thenReturn(profile);

        AppLoveUserProfileUpdateBasicReqVO reqVO = new AppLoveUserProfileUpdateBasicReqVO();
        reqVO.setRealName("张三");
        reqVO.setGender(1);
        reqVO.setBirthday(LocalDate.of(1995, 5, 1));
        reqVO.setCityCode("430626");
        reqVO.setCityName("平江县");
        reqVO.setMaritalStatus(1);

        loveUserProfileService.updateBasicProfile(1L, reqVO);

        assertEquals("张三", profile.getRealName());
        assertEquals(1, profile.getGender());
        assertEquals(40, profile.getCompletionRate());
        verify(loveUserProfileMapper).updateById(profile);
    }

    @Test
    public void testUpdateBasicProfileCreateIfAbsent() {
        when(loveUserProfileMapper.selectOne(anySFunction(), eq(2L))).thenReturn(null);

        AppLoveUserProfileUpdateBasicReqVO reqVO = new AppLoveUserProfileUpdateBasicReqVO();
        reqVO.setGender(2);
        reqVO.setBirthday(LocalDate.of(1996, 6, 1));
        reqVO.setCityCode("310000");
        reqVO.setCityName("上海市");
        reqVO.setMaritalStatus(1);

        loveUserProfileService.updateBasicProfile(2L, reqVO);

        verify(loveUserProfileMapper).insert(any(LoveUserProfileDO.class));
        verify(loveUserProfileMapper).updateById(any(LoveUserProfileDO.class));
    }

    @SuppressWarnings("unchecked")
    private static SFunction<LoveUserProfileDO, ?> anySFunction() {
        return (SFunction<LoveUserProfileDO, ?>) any(SFunction.class);
    }
}

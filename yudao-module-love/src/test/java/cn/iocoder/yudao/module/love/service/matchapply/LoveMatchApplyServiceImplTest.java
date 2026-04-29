package cn.iocoder.yudao.module.love.service.matchapply;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyCreateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchApplyDO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchApplyLogDO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchmakerDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.match.LoveMatchApplyLogMapper;
import cn.iocoder.yudao.module.love.dal.mysql.match.LoveMatchApplyMapper;
import cn.iocoder.yudao.module.love.dal.mysql.match.LoveMatchmakerMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoveMatchApplyServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private LoveMatchApplyServiceImpl loveMatchApplyService;

    @Mock
    private LoveMatchApplyMapper loveMatchApplyMapper;
    @Mock
    private LoveMatchApplyLogMapper loveMatchApplyLogMapper;
    @Mock
    private LoveMatchmakerMapper loveMatchmakerMapper;
    @Mock
    private LoveUserMapper loveUserMapper;
    @Mock
    private LoveUserProfileMapper loveUserProfileMapper;

    @Test
    public void testCreateApply_rejectSelfApply() {
        AppLoveMatchApplyCreateReqVO reqVO = new AppLoveMatchApplyCreateReqVO();
        reqVO.setTargetUserId(1L);

        assertServiceException(() -> loveMatchApplyService.createApply(1L, reqVO),
                LOVE_MATCH_APPLY_SELF_NOT_ALLOWED);
    }

    @Test
    public void testCreateApply_rejectDuplicateApply() {
        AppLoveMatchApplyCreateReqVO reqVO = new AppLoveMatchApplyCreateReqVO();
        reqVO.setTargetUserId(2L);
        when(loveMatchApplyMapper.selectDuplicateCount(1L, 2L)).thenReturn(1L);

        assertServiceException(() -> loveMatchApplyService.createApply(1L, reqVO),
                LOVE_MATCH_APPLY_DUPLICATE);
    }

    @Test
    public void testCreateApply_rejectUnauthenticatedProfile() {
        AppLoveMatchApplyCreateReqVO reqVO = new AppLoveMatchApplyCreateReqVO();
        reqVO.setTargetUserId(2L);
        when(loveMatchApplyMapper.selectDuplicateCount(1L, 2L)).thenReturn(0L);
        when(loveUserMapper.selectById(1L)).thenReturn(LoveUserDO.builder().id(1L).authStatus(0).freeMatchQuota(1).build());

        assertServiceException(() -> loveMatchApplyService.createApply(1L, reqVO),
                LOVE_MATCH_APPLY_AUTH_REQUIRED);
    }

    @Test
    public void testCreateApply_assignDefaultMatchmakerAndDecrementQuota() {
        AppLoveMatchApplyCreateReqVO reqVO = new AppLoveMatchApplyCreateReqVO();
        reqVO.setTargetUserId(2L);
        reqVO.setApplyReason("想认识一下");
        when(loveMatchApplyMapper.selectDuplicateCount(1L, 2L)).thenReturn(0L);
        LoveUserDO user = LoveUserDO.builder().id(1L).authStatus(1).freeMatchQuota(1).build();
        when(loveUserMapper.selectById(1L)).thenReturn(user);
        LoveUserProfileDO profile = LoveUserProfileDO.builder()
                .userId(1L)
                .gender(1)
                .birthday(LocalDate.of(1995, 5, 1))
                .cityCode("430626")
                .cityName("平江县")
                .maritalStatus(1)
                .build();
        when(loveUserProfileMapper.selectOne(anySFunction(), eq(1L))).thenReturn(profile);
        when(loveMatchmakerMapper.selectDefaultMatchmaker()).thenReturn(LoveMatchmakerDO.builder().id(9L).build());
        doAnswer(invocation -> {
            LoveMatchApplyDO apply = invocation.getArgument(0);
            apply.setId(100L);
            return 1;
        }).when(loveMatchApplyMapper).insert(any(LoveMatchApplyDO.class));
        doAnswer(invocation -> {
            LoveMatchApplyLogDO log = invocation.getArgument(0);
            log.setId(200L);
            return 1;
        }).when(loveMatchApplyLogMapper).insert(any(LoveMatchApplyLogDO.class));

        Long applyId = loveMatchApplyService.createApply(1L, reqVO);

        assertEquals(100L, applyId);
        assertEquals(0, user.getFreeMatchQuota());
        verify(loveMatchApplyMapper).updateById(any(LoveMatchApplyDO.class));
        verify(loveUserMapper).updateById(user);
    }

    @Test
    public void testStartContact_transitionSuccess() {
        LoveMatchApplyDO apply = LoveMatchApplyDO.builder()
                .id(10L)
                .status(10)
                .build();
        when(loveMatchApplyMapper.selectById(10L)).thenReturn(apply);
        doAnswer(invocation -> {
            LoveMatchApplyLogDO log = invocation.getArgument(0);
            log.setId(300L);
            return 1;
        }).when(loveMatchApplyLogMapper).insert(any(LoveMatchApplyLogDO.class));

        loveMatchApplyService.startContact(10L, 99L, "已联系对方");

        assertEquals(20, apply.getStatus());
        assertNotNull(apply.getProcessedAt());
        verify(loveMatchApplyMapper).updateById(apply);
    }

    @SuppressWarnings("unchecked")
    private static SFunction<LoveUserProfileDO, ?> anySFunction() {
        return (SFunction<LoveUserProfileDO, ?>) any(SFunction.class);
    }
}

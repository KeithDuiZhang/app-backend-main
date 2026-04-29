package cn.iocoder.yudao.module.love.service.matchapply;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.constant.LoveBizConstants;
import cn.iocoder.yudao.module.love.controller.admin.matchapply.vo.LoveMatchApplyPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyCreateReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyRespVO;
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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.*;

@Service
public class LoveMatchApplyServiceImpl implements LoveMatchApplyService {

    private static final int USER_AUTH_VERIFIED = 1;
    private static final int STATUS_INIT = 0;
    private static final int STATUS_WAIT_MATCHMAKER = 10;
    private static final int STATUS_CONTACTING = 20;
    private static final int STATUS_SUCCESS = 30;
    private static final int STATUS_REJECTED = 40;
    private static final int STATUS_CLOSED = 50;
    private static final int OPERATOR_SYSTEM = 0;
    private static final int OPERATOR_ADMIN = 1;

    @Resource
    private LoveMatchApplyMapper loveMatchApplyMapper;
    @Resource
    private LoveMatchApplyLogMapper loveMatchApplyLogMapper;
    @Resource
    private LoveMatchmakerMapper loveMatchmakerMapper;
    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveUserProfileMapper loveUserProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createApply(Long currentUserId, AppLoveMatchApplyCreateReqVO reqVO) {
        Long targetUserId = reqVO.getTargetUserId();
        if (currentUserId.equals(targetUserId)) {
            throw exception(LOVE_MATCH_APPLY_SELF_NOT_ALLOWED);
        }
        if (loveMatchApplyMapper.selectDuplicateCount(currentUserId, targetUserId) > 0) {
            throw exception(LOVE_MATCH_APPLY_DUPLICATE);
        }
        LoveUserDO user = loveUserMapper.selectById(currentUserId);
        if (user == null || !Integer.valueOf(USER_AUTH_VERIFIED).equals(user.getAuthStatus())) {
            throw exception(LOVE_MATCH_APPLY_AUTH_REQUIRED);
        }
        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, currentUserId);
        if (!isBasicProfileCompleted(profile)) {
            throw exception(LOVE_MATCH_APPLY_PROFILE_REQUIRED);
        }
        boolean memberActive = isMemberActive(user);
        if (!memberActive && (user.getFreeMatchQuota() == null || user.getFreeMatchQuota() <= 0)) {
            throw exception(LOVE_MATCH_APPLY_QUOTA_EMPTY);
        }
        LoveMatchmakerDO matchmaker = loveMatchmakerMapper.selectDefaultMatchmaker();
        if (matchmaker == null) {
            throw exception(LOVE_MATCHMAKER_DEFAULT_NOT_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        LoveMatchApplyDO apply = LoveMatchApplyDO.builder()
                .fromUserId(currentUserId)
                .toUserId(targetUserId)
                .matchmakerId(matchmaker.getId())
                .status(STATUS_WAIT_MATCHMAKER)
                .rejectReason(null)
                .submittedAt(now)
                .processedAt(null)
                .applyReason(reqVO.getApplyReason())
                .sourceType(0)
                .build();
        loveMatchApplyMapper.insert(apply);

        LoveMatchApplyLogDO log = buildLog(apply.getId(), STATUS_INIT, STATUS_WAIT_MATCHMAKER, OPERATOR_SYSTEM, null, "自动分配默认红娘");
        loveMatchApplyLogMapper.insert(log);
        apply.setLatestLogId(log.getId());
        loveMatchApplyMapper.updateById(apply);

        if (!memberActive) {
            user.setFreeMatchQuota(user.getFreeMatchQuota() - 1);
            loveUserMapper.updateById(user);
        }
        return apply.getId();
    }

    @Override
    public PageResult<AppLoveMatchApplyRespVO> getMyApplyPage(Long currentUserId, AppLoveMatchApplyPageReqVO reqVO) {
        PageResult<LoveMatchApplyDO> pageResult = loveMatchApplyMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMatchApplyDO>()
                .eq(LoveMatchApplyDO::getFromUserId, currentUserId)
                .eqIfPresent(LoveMatchApplyDO::getStatus, reqVO.getStatus())
                .orderByDesc(LoveMatchApplyDO::getId));
        return new PageResult<>(pageResult.getList().stream().map(this::convertAppResp).toList(), pageResult.getTotal());
    }

    @Override
    public AppLoveMatchApplyRespVO getMyApply(Long currentUserId, Long id) {
        LoveMatchApplyDO apply = loveMatchApplyMapper.selectById(id);
        if (apply == null || !currentUserId.equals(apply.getFromUserId())) {
            throw exception(LOVE_MATCH_APPLY_NOT_EXISTS);
        }
        return convertAppResp(apply);
    }

    @Override
    public PageResult<LoveMatchApplyDO> getApplyPage(LoveMatchApplyPageReqVO reqVO) {
        return loveMatchApplyMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMatchApplyDO>()
                .eqIfPresent(LoveMatchApplyDO::getFromUserId, reqVO.getFromUserId())
                .eqIfPresent(LoveMatchApplyDO::getToUserId, reqVO.getToUserId())
                .eqIfPresent(LoveMatchApplyDO::getMatchmakerId, reqVO.getMatchmakerId())
                .eqIfPresent(LoveMatchApplyDO::getStatus, reqVO.getStatus())
                .orderByDesc(LoveMatchApplyDO::getId));
    }

    @Override
    public LoveMatchApplyDO getApply(Long id) {
        LoveMatchApplyDO apply = loveMatchApplyMapper.selectById(id);
        if (apply == null) {
            throw exception(LOVE_MATCH_APPLY_NOT_EXISTS);
        }
        return apply;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startContact(Long id, Long operatorId, String remark) {
        LoveMatchApplyDO apply = getApply(id);
        validateStatus(apply, STATUS_WAIT_MATCHMAKER);
        transition(apply, STATUS_CONTACTING, operatorId, appendRemark("开始联系", remark));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSuccess(Long id, Long operatorId, String remark) {
        LoveMatchApplyDO apply = getApply(id);
        validateStatus(apply, STATUS_CONTACTING);
        transition(apply, STATUS_SUCCESS, operatorId, appendRemark("牵线成功", remark));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRejected(Long id, Long operatorId, String rejectReason) {
        LoveMatchApplyDO apply = getApply(id);
        validateStatus(apply, STATUS_CONTACTING);
        apply.setRejectReason(rejectReason);
        transition(apply, STATUS_REJECTED, operatorId, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeApply(Long id, Long operatorId, String remark) {
        LoveMatchApplyDO apply = getApply(id);
        if (!(STATUS_WAIT_MATCHMAKER == apply.getStatus() || STATUS_CONTACTING == apply.getStatus())) {
            throw exception(LOVE_MATCH_APPLY_STATUS_INVALID);
        }
        transition(apply, STATUS_CLOSED, operatorId, appendRemark("关闭申请", remark));
    }

    private void transition(LoveMatchApplyDO apply, int toStatus, Long operatorId, String remark) {
        Integer fromStatus = apply.getStatus();
        apply.setStatus(toStatus);
        apply.setProcessedAt(LocalDateTime.now());
        LoveMatchApplyLogDO log = buildLog(apply.getId(), fromStatus, toStatus, OPERATOR_ADMIN, operatorId, remark);
        loveMatchApplyLogMapper.insert(log);
        apply.setLatestLogId(log.getId());
        loveMatchApplyMapper.updateById(apply);
    }

    private void validateStatus(LoveMatchApplyDO apply, int expectedStatus) {
        if (apply.getStatus() == null || apply.getStatus() != expectedStatus) {
            throw exception(LOVE_MATCH_APPLY_STATUS_INVALID);
        }
    }

    private boolean isBasicProfileCompleted(LoveUserProfileDO profile) {
        return profile != null
                && profile.getGender() != null
                && profile.getBirthday() != null
                && StrUtil.isNotBlank(profile.getCityCode())
                && StrUtil.isNotBlank(profile.getCityName())
                && profile.getMaritalStatus() != null;
    }

    private LoveMatchApplyLogDO buildLog(Long applyId, Integer fromStatus, Integer toStatus, Integer operatorType,
                                         Long operatorId, String remark) {
        return LoveMatchApplyLogDO.builder()
                .applyId(applyId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .operatorType(operatorType)
                .operatorId(operatorId)
                .remark(remark)
                .build();
    }

    private AppLoveMatchApplyRespVO convertAppResp(LoveMatchApplyDO apply) {
        AppLoveMatchApplyRespVO respVO = new AppLoveMatchApplyRespVO();
        respVO.setId(apply.getId());
        respVO.setFromUserId(apply.getFromUserId());
        respVO.setToUserId(apply.getToUserId());
        respVO.setMatchmakerId(apply.getMatchmakerId());
        respVO.setStatus(apply.getStatus());
        respVO.setRejectReason(apply.getRejectReason());
        respVO.setApplyReason(apply.getApplyReason());
        respVO.setSubmittedAt(apply.getSubmittedAt());
        respVO.setProcessedAt(apply.getProcessedAt());
        return respVO;
    }

    private String appendRemark(String prefix, String remark) {
        return StrUtil.isBlank(remark) ? prefix : prefix + "：" + remark;
    }

    private boolean isMemberActive(LoveUserDO user) {
        if (user.getMemberLevel() == null || user.getMemberLevel() <= LoveBizConstants.MEMBER_LEVEL_NONE) {
            return false;
        }
        if (user.getMemberExpireTime() != null && user.getMemberExpireTime().isAfter(LocalDateTime.now())) {
            return true;
        }
        user.setMemberLevel(LoveBizConstants.MEMBER_LEVEL_NONE);
        user.setMemberExpireTime(null);
        loveUserMapper.updateById(user);
        return false;
    }
}

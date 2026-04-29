package cn.iocoder.yudao.module.love.service.recommend;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserProfileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
public class LoveRecommendUserServiceImpl implements LoveRecommendUserService {

    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveUserProfileMapper loveUserProfileMapper;

    @Override
    public PageResult<AppLoveRecommendUserRespVO> getRecommendPage(AppLoveRecommendUserPageReqVO reqVO) {
        LocalDate today = LocalDate.now();
        LocalDate birthdayStart = reqVO.getMaxAge() != null ? today.minusYears(reqVO.getMaxAge() + 1L).plusDays(1) : null;
        LocalDate birthdayEnd = reqVO.getMinAge() != null ? today.minusYears(reqVO.getMinAge()) : null;
        PageResult<LoveUserProfileDO> profilePage = loveUserProfileMapper.selectPage(reqVO,
                new LambdaQueryWrapperX<LoveUserProfileDO>()
                        .eq(LoveUserProfileDO::getProfilePublic, Boolean.TRUE)
                        .eqIfPresent(LoveUserProfileDO::getGender, reqVO.getGender())
                        .eqIfPresent(LoveUserProfileDO::getMaritalStatus, reqVO.getMaritalStatus())
                        .betweenIfPresent(LoveUserProfileDO::getBirthday, birthdayStart, birthdayEnd)
                        .eq(Boolean.TRUE.equals(reqVO.getSameCity()) && reqVO.getCityName() != null,
                                LoveUserProfileDO::getCityName, reqVO.getCityName())
                        .ne(Boolean.FALSE.equals(reqVO.getSameCity()) && reqVO.getCityName() != null,
                                LoveUserProfileDO::getCityName, reqVO.getCityName())
                        .orderByDesc(LoveUserProfileDO::getId));
        List<LoveUserProfileDO> profiles = profilePage.getList();
        if (profiles.isEmpty()) {
            return new PageResult<>(List.of(), profilePage.getTotal());
        }
        Map<Long, LoveUserDO> userMap = loveUserMapper.selectList(LoveUserDO::getId,
                        profiles.stream().map(LoveUserProfileDO::getUserId).toList())
                .stream()
                .collect(Collectors.toMap(LoveUserDO::getId, Function.identity()));
        return new PageResult<>(profiles.stream()
                .map(profile -> buildResp(userMap.get(profile.getUserId()), profile))
                .filter(Objects::nonNull)
                .toList(), profilePage.getTotal());
    }

    @Override
    public AppLoveRecommendUserRespVO getRecommendUser(Long userId) {
        LoveUserDO user = loveUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        LoveUserProfileDO profile = loveUserProfileMapper.selectOne(LoveUserProfileDO::getUserId, userId);
        return buildResp(user, profile);
    }

    private AppLoveRecommendUserRespVO buildResp(LoveUserDO user, LoveUserProfileDO profile) {
        if (user == null || profile == null) {
            return null;
        }
        AppLoveRecommendUserRespVO respVO = new AppLoveRecommendUserRespVO();
        respVO.setUserId(user.getId());
        respVO.setNickname(user.getNickname());
        respVO.setAvatar(user.getAvatar());
        respVO.setGender(profile.getGender());
        respVO.setAge(profile.getBirthday() != null ? Period.between(profile.getBirthday(), LocalDate.now()).getYears() : null);
        respVO.setCityName(profile.getCityName());
        respVO.setMaritalStatus(profile.getMaritalStatus());
        respVO.setBio(profile.getBio());
        respVO.setTags(profile.getTags());
        respVO.setAuthStatus(user.getAuthStatus());
        respVO.setMemberLevel(user.getMemberLevel());
        return respVO;
    }
}

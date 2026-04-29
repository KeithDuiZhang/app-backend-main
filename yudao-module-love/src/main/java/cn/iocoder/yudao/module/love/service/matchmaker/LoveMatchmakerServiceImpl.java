package cn.iocoder.yudao.module.love.service.matchmaker;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerSaveReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchmakerDO;
import cn.iocoder.yudao.module.love.dal.mysql.match.LoveMatchmakerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MATCHMAKER_NOT_EXISTS;

@Service
public class LoveMatchmakerServiceImpl implements LoveMatchmakerService {

    @Resource
    private LoveMatchmakerMapper loveMatchmakerMapper;

    @Override
    public PageResult<LoveMatchmakerDO> getMatchmakerPage(LoveMatchmakerPageReqVO reqVO) {
        return loveMatchmakerMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMatchmakerDO>()
                .likeIfPresent(LoveMatchmakerDO::getName, reqVO.getName())
                .eqIfPresent(LoveMatchmakerDO::getStatus, reqVO.getStatus())
                .orderByDesc(LoveMatchmakerDO::getId));
    }

    @Override
    public LoveMatchmakerDO getMatchmaker(Long id) {
        return loveMatchmakerMapper.selectById(id);
    }

    @Override
    public Long createMatchmaker(LoveMatchmakerSaveReqVO reqVO) {
        LoveMatchmakerDO matchmaker = BeanUtils.toBean(reqVO, LoveMatchmakerDO.class);
        loveMatchmakerMapper.insert(matchmaker);
        if (Boolean.TRUE.equals(reqVO.getIsDefault())) {
            updateDefaultMatchmaker(matchmaker.getId());
        }
        return matchmaker.getId();
    }

    @Override
    public void updateMatchmaker(LoveMatchmakerSaveReqVO reqVO) {
        validateMatchmakerExists(reqVO.getId());
        LoveMatchmakerDO matchmaker = BeanUtils.toBean(reqVO, LoveMatchmakerDO.class);
        loveMatchmakerMapper.updateById(matchmaker);
        if (Boolean.TRUE.equals(reqVO.getIsDefault())) {
            updateDefaultMatchmaker(reqVO.getId());
        }
    }

    @Override
    public void updateDefaultMatchmaker(Long id) {
        validateMatchmakerExists(id);
        List<LoveMatchmakerDO> defaultList = loveMatchmakerMapper.selectList(LoveMatchmakerDO::getIsDefault, Boolean.TRUE);
        defaultList.forEach(item -> {
            if (!item.getId().equals(id)) {
                item.setIsDefault(Boolean.FALSE);
                loveMatchmakerMapper.updateById(item);
            }
        });
        LoveMatchmakerDO target = loveMatchmakerMapper.selectById(id);
        if (!Boolean.TRUE.equals(target.getIsDefault())) {
            target.setIsDefault(Boolean.TRUE);
            loveMatchmakerMapper.updateById(target);
        }
    }

    private void validateMatchmakerExists(Long id) {
        if (id == null || loveMatchmakerMapper.selectById(id) == null) {
            throw exception(LOVE_MATCHMAKER_NOT_EXISTS);
        }
    }
}

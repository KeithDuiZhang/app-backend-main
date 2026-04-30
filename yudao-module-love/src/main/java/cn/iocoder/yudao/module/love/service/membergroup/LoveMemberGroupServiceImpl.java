package cn.iocoder.yudao.module.love.service.membergroup;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberGroupDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberGroupMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_GROUP_NOT_EXISTS;

@Service
public class LoveMemberGroupServiceImpl implements LoveMemberGroupService {

    @Resource
    private LoveMemberGroupMapper loveMemberGroupMapper;

    @Override
    public LoveMemberGroupDO getMemberGroup(Long id) {
        LoveMemberGroupDO group = loveMemberGroupMapper.selectById(id);
        if (group == null) {
            throw exception(LOVE_MEMBER_GROUP_NOT_EXISTS);
        }
        return group;
    }

    @Override
    public PageResult<LoveMemberGroupDO> getMemberGroupPage(LoveMemberGroupPageReqVO reqVO) {
        return loveMemberGroupMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMemberGroupDO>()
                .likeIfPresent(LoveMemberGroupDO::getCode, reqVO.getCode())
                .likeIfPresent(LoveMemberGroupDO::getName, reqVO.getName())
                .eqIfPresent(LoveMemberGroupDO::getStatus, reqVO.getStatus())
                .orderByAsc(LoveMemberGroupDO::getSort)
                .orderByDesc(LoveMemberGroupDO::getId));
    }

    @Override
    public List<LoveMemberGroupDO> getEnabledMemberGroups() {
        return loveMemberGroupMapper.selectList(new LambdaQueryWrapperX<LoveMemberGroupDO>()
                .eq(LoveMemberGroupDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(LoveMemberGroupDO::getSort));
    }

    @Override
    public Long createMemberGroup(LoveMemberGroupCreateReqVO reqVO) {
        LoveMemberGroupDO group = toGroup(reqVO);
        loveMemberGroupMapper.insert(group);
        return group.getId();
    }

    @Override
    public void updateMemberGroup(LoveMemberGroupUpdateReqVO reqVO) {
        getMemberGroup(reqVO.getId());
        LoveMemberGroupDO group = toGroup(reqVO);
        group.setId(reqVO.getId());
        loveMemberGroupMapper.updateById(group);
    }

    @Override
    public void deleteMemberGroup(Long id) {
        getMemberGroup(id);
        loveMemberGroupMapper.deleteById(id);
    }

    private LoveMemberGroupDO toGroup(LoveMemberGroupCreateReqVO reqVO) {
        return LoveMemberGroupDO.builder()
                .code(reqVO.getCode())
                .name(reqVO.getName())
                .level(reqVO.getLevel())
                .theme(reqVO.getTheme())
                .benefitsJson(reqVO.getBenefitsJson())
                .sort(reqVO.getSort())
                .status(reqVO.getStatus())
                .build();
    }

    private LoveMemberGroupDO toGroup(LoveMemberGroupUpdateReqVO reqVO) {
        return LoveMemberGroupDO.builder()
                .code(reqVO.getCode())
                .name(reqVO.getName())
                .level(reqVO.getLevel())
                .theme(reqVO.getTheme())
                .benefitsJson(reqVO.getBenefitsJson())
                .sort(reqVO.getSort())
                .status(reqVO.getStatus())
                .build();
    }
}

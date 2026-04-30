package cn.iocoder.yudao.module.love.service.membergroup;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membergroup.vo.LoveMemberGroupUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberGroupDO;

import java.util.List;

public interface LoveMemberGroupService {

    LoveMemberGroupDO getMemberGroup(Long id);

    PageResult<LoveMemberGroupDO> getMemberGroupPage(LoveMemberGroupPageReqVO reqVO);

    List<LoveMemberGroupDO> getEnabledMemberGroups();

    Long createMemberGroup(LoveMemberGroupCreateReqVO reqVO);

    void updateMemberGroup(LoveMemberGroupUpdateReqVO reqVO);

    void deleteMemberGroup(Long id);
}

package cn.iocoder.yudao.module.love.service.matchmaker;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.matchmaker.vo.LoveMatchmakerSaveReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchmakerDO;

public interface LoveMatchmakerService {

    PageResult<LoveMatchmakerDO> getMatchmakerPage(LoveMatchmakerPageReqVO reqVO);

    LoveMatchmakerDO getMatchmaker(Long id);

    Long createMatchmaker(LoveMatchmakerSaveReqVO reqVO);

    void updateMatchmaker(LoveMatchmakerSaveReqVO reqVO);

    void updateDefaultMatchmaker(Long id);
}

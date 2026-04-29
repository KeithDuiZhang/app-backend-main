package cn.iocoder.yudao.module.love.service.matchapply;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.matchapply.vo.LoveMatchApplyPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyCreateReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.matchapply.vo.AppLoveMatchApplyRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchApplyDO;

public interface LoveMatchApplyService {

    Long createApply(Long currentUserId, AppLoveMatchApplyCreateReqVO reqVO);

    PageResult<AppLoveMatchApplyRespVO> getMyApplyPage(Long currentUserId, AppLoveMatchApplyPageReqVO reqVO);

    AppLoveMatchApplyRespVO getMyApply(Long currentUserId, Long id);

    PageResult<LoveMatchApplyDO> getApplyPage(LoveMatchApplyPageReqVO reqVO);

    LoveMatchApplyDO getApply(Long id);

    void startContact(Long id, Long operatorId, String remark);

    void markSuccess(Long id, Long operatorId, String remark);

    void markRejected(Long id, Long operatorId, String rejectReason);

    void closeApply(Long id, Long operatorId, String remark);
}

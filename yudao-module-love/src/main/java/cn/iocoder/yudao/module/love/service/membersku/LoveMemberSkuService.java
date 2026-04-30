package cn.iocoder.yudao.module.love.service.membersku;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberSkuDO;

public interface LoveMemberSkuService {

    LoveMemberSkuDO getMemberSku(Long id);

    PageResult<LoveMemberSkuDO> getMemberSkuPage(LoveMemberSkuPageReqVO reqVO);

    Long createMemberSku(LoveMemberSkuCreateReqVO reqVO);

    void updateMemberSku(LoveMemberSkuUpdateReqVO reqVO);

    void deleteMemberSku(Long id);
}

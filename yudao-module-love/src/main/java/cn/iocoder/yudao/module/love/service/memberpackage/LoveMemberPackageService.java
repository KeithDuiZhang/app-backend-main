package cn.iocoder.yudao.module.love.service.memberpackage;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackagePageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageUpdateReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberpackage.vo.AppLoveMemberPackageRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberPackageDO;

import java.util.List;

public interface LoveMemberPackageService {

    LoveMemberPackageDO validateMemberPackage(Long id);

    LoveMemberPackageDO getMemberPackage(Long id);

    PageResult<LoveMemberPackageDO> getMemberPackagePage(LoveMemberPackagePageReqVO reqVO);

    List<AppLoveMemberPackageRespVO> getEnableMemberPackageList();

    Long createMemberPackage(LoveMemberPackageCreateReqVO reqVO);

    void updateMemberPackage(LoveMemberPackageUpdateReqVO reqVO);

    void deleteMemberPackage(Long id);
}

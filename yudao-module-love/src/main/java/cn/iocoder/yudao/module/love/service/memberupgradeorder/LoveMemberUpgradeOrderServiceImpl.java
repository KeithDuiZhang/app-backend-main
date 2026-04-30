package cn.iocoder.yudao.module.love.service.memberupgradeorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.admin.memberupgradeorder.vo.LoveMemberUpgradeOrderPageReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberUpgradeOrderDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberUpgradeOrderMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class LoveMemberUpgradeOrderServiceImpl implements LoveMemberUpgradeOrderService {

    @Resource
    private LoveMemberUpgradeOrderMapper loveMemberUpgradeOrderMapper;

    @Override
    public PageResult<LoveMemberUpgradeOrderDO> getUpgradeOrderPage(LoveMemberUpgradeOrderPageReqVO reqVO) {
        return loveMemberUpgradeOrderMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMemberUpgradeOrderDO>()
                .eqIfPresent(LoveMemberUpgradeOrderDO::getUserId, reqVO.getUserId())
                .eqIfPresent(LoveMemberUpgradeOrderDO::getStatus, reqVO.getStatus())
                .likeIfPresent(LoveMemberUpgradeOrderDO::getOrderNo, reqVO.getOrderNo())
                .orderByDesc(LoveMemberUpgradeOrderDO::getId));
    }
}

package cn.iocoder.yudao.module.love.service.membersku;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuPageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.membersku.vo.LoveMemberSkuUpdateReqVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberSkuDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberSkuMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_SKU_NOT_EXISTS;

@Service
public class LoveMemberSkuServiceImpl implements LoveMemberSkuService {

    @Resource
    private LoveMemberSkuMapper loveMemberSkuMapper;

    @Override
    public LoveMemberSkuDO getMemberSku(Long id) {
        LoveMemberSkuDO sku = loveMemberSkuMapper.selectById(id);
        if (sku == null) {
            throw exception(LOVE_MEMBER_SKU_NOT_EXISTS);
        }
        return sku;
    }

    @Override
    public PageResult<LoveMemberSkuDO> getMemberSkuPage(LoveMemberSkuPageReqVO reqVO) {
        return loveMemberSkuMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMemberSkuDO>()
                .eqIfPresent(LoveMemberSkuDO::getGroupId, reqVO.getGroupId())
                .eqIfPresent(LoveMemberSkuDO::getDurationType, reqVO.getDurationType())
                .eqIfPresent(LoveMemberSkuDO::getStatus, reqVO.getStatus())
                .orderByAsc(LoveMemberSkuDO::getSort)
                .orderByDesc(LoveMemberSkuDO::getId));
    }

    @Override
    public Long createMemberSku(LoveMemberSkuCreateReqVO reqVO) {
        LoveMemberSkuDO sku = toSku(reqVO);
        loveMemberSkuMapper.insert(sku);
        return sku.getId();
    }

    @Override
    public void updateMemberSku(LoveMemberSkuUpdateReqVO reqVO) {
        getMemberSku(reqVO.getId());
        LoveMemberSkuDO sku = toSku(reqVO);
        sku.setId(reqVO.getId());
        loveMemberSkuMapper.updateById(sku);
    }

    @Override
    public void deleteMemberSku(Long id) {
        getMemberSku(id);
        loveMemberSkuMapper.deleteById(id);
    }

    private LoveMemberSkuDO toSku(LoveMemberSkuCreateReqVO reqVO) {
        return LoveMemberSkuDO.builder()
                .groupId(reqVO.getGroupId())
                .durationType(reqVO.getDurationType())
                .durationDays(reqVO.getDurationDays())
                .salePriceFen(reqVO.getSalePriceFen())
                .originalPriceFen(reqVO.getOriginalPriceFen())
                .tagText(reqVO.getTagText())
                .recommend(reqVO.getRecommend())
                .sort(reqVO.getSort())
                .status(reqVO.getStatus())
                .build();
    }

    private LoveMemberSkuDO toSku(LoveMemberSkuUpdateReqVO reqVO) {
        return LoveMemberSkuDO.builder()
                .groupId(reqVO.getGroupId())
                .durationType(reqVO.getDurationType())
                .durationDays(reqVO.getDurationDays())
                .salePriceFen(reqVO.getSalePriceFen())
                .originalPriceFen(reqVO.getOriginalPriceFen())
                .tagText(reqVO.getTagText())
                .recommend(reqVO.getRecommend())
                .sort(reqVO.getSort())
                .status(reqVO.getStatus())
                .build();
    }
}

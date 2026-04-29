package cn.iocoder.yudao.module.love.service.memberpackage;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageBaseReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageCreateReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackagePageReqVO;
import cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo.LoveMemberPackageUpdateReqVO;
import cn.iocoder.yudao.module.love.controller.app.memberpackage.vo.AppLoveMemberPackageRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberPackageDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberPackageMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_PACKAGE_DISABLED;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_PACKAGE_NAME_EXISTS;
import static cn.iocoder.yudao.module.love.enums.ErrorCodeConstants.LOVE_MEMBER_PACKAGE_NOT_EXISTS;

@Service
public class LoveMemberPackageServiceImpl implements LoveMemberPackageService {

    @Resource
    private LoveMemberPackageMapper loveMemberPackageMapper;

    @Override
    public LoveMemberPackageDO validateMemberPackage(Long id) {
        LoveMemberPackageDO memberPackage = getMemberPackage(id);
        if (memberPackage == null) {
            throw exception(LOVE_MEMBER_PACKAGE_NOT_EXISTS);
        }
        if (CommonStatusEnum.DISABLE.getStatus().equals(memberPackage.getStatus())) {
            throw exception(LOVE_MEMBER_PACKAGE_DISABLED);
        }
        return memberPackage;
    }

    @Override
    public LoveMemberPackageDO getMemberPackage(Long id) {
        return loveMemberPackageMapper.selectById(id);
    }

    @Override
    public PageResult<LoveMemberPackageDO> getMemberPackagePage(LoveMemberPackagePageReqVO reqVO) {
        return loveMemberPackageMapper.selectPage(reqVO, new LambdaQueryWrapperX<LoveMemberPackageDO>()
                .likeIfPresent(LoveMemberPackageDO::getName, reqVO.getName())
                .eqIfPresent(LoveMemberPackageDO::getLevel, reqVO.getLevel())
                .eqIfPresent(LoveMemberPackageDO::getStatus, reqVO.getStatus())
                .orderByAsc(LoveMemberPackageDO::getSort)
                .orderByDesc(LoveMemberPackageDO::getId));
    }

    @Override
    public List<AppLoveMemberPackageRespVO> getEnableMemberPackageList() {
        List<LoveMemberPackageDO> packages = loveMemberPackageMapper.selectList(new LambdaQueryWrapperX<LoveMemberPackageDO>()
                .eq(LoveMemberPackageDO::getStatus, CommonStatusEnum.ENABLE.getStatus())
                .orderByAsc(LoveMemberPackageDO::getSort)
                .orderByAsc(LoveMemberPackageDO::getPriceFen));
        return packages.stream().map(this::convertAppResp).sorted(Comparator.comparingInt(AppLoveMemberPackageRespVO::getSort)).toList();
    }

    @Override
    public Long createMemberPackage(LoveMemberPackageCreateReqVO reqVO) {
        validatePackageNameUnique(null, reqVO.getName());
        LoveMemberPackageDO memberPackage = buildDO(reqVO);
        loveMemberPackageMapper.insert(memberPackage);
        return memberPackage.getId();
    }

    @Override
    public void updateMemberPackage(LoveMemberPackageUpdateReqVO reqVO) {
        if (loveMemberPackageMapper.selectById(reqVO.getId()) == null) {
            throw exception(LOVE_MEMBER_PACKAGE_NOT_EXISTS);
        }
        validatePackageNameUnique(reqVO.getId(), reqVO.getName());
        LoveMemberPackageDO memberPackage = buildDO(reqVO);
        memberPackage.setId(reqVO.getId());
        loveMemberPackageMapper.updateById(memberPackage);
    }

    @Override
    public void deleteMemberPackage(Long id) {
        if (loveMemberPackageMapper.selectById(id) == null) {
            throw exception(LOVE_MEMBER_PACKAGE_NOT_EXISTS);
        }
        loveMemberPackageMapper.deleteById(id);
    }

    private void validatePackageNameUnique(Long id, String name) {
        LoveMemberPackageDO existing = loveMemberPackageMapper.selectOne(new LambdaQueryWrapperX<LoveMemberPackageDO>()
                .eq(LoveMemberPackageDO::getName, name)
                .last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        if (id == null || !id.equals(existing.getId())) {
            throw exception(LOVE_MEMBER_PACKAGE_NAME_EXISTS);
        }
    }

    private LoveMemberPackageDO buildDO(LoveMemberPackageBaseReqVO reqVO) {
        LoveMemberPackageDO memberPackage = new LoveMemberPackageDO();
        memberPackage.setName(reqVO.getName());
        memberPackage.setLevel(reqVO.getLevel());
        memberPackage.setPriceFen(reqVO.getPriceFen());
        memberPackage.setDurationMonths(reqVO.getDurationMonths());
        memberPackage.setDescription(reqVO.getDescription());
        memberPackage.setFeaturesJson(normalizeFeaturesJson(reqVO.getFeaturesJson()));
        memberPackage.setTheme(StrUtil.blankToDefault(reqVO.getTheme(), "silver"));
        memberPackage.setPopular(Boolean.TRUE.equals(reqVO.getPopular()));
        memberPackage.setSort(reqVO.getSort());
        memberPackage.setStatus(reqVO.getStatus());
        return memberPackage;
    }

    private AppLoveMemberPackageRespVO convertAppResp(LoveMemberPackageDO item) {
        AppLoveMemberPackageRespVO respVO = new AppLoveMemberPackageRespVO();
        respVO.setId(item.getId());
        respVO.setName(item.getName());
        respVO.setLevel(item.getLevel());
        respVO.setPriceFen(item.getPriceFen());
        respVO.setDurationMonths(item.getDurationMonths());
        respVO.setDurationText(toDurationText(item.getDurationMonths()));
        respVO.setDescription(item.getDescription());
        respVO.setFeatures(parseFeatures(item.getFeaturesJson()));
        respVO.setTheme(item.getTheme());
        respVO.setPopular(Boolean.TRUE.equals(item.getPopular()));
        respVO.setSort(item.getSort());
        return respVO;
    }

    private String normalizeFeaturesJson(String featuresJson) {
        if (StrUtil.isBlank(featuresJson)) {
            return "[]";
        }
        List<String> features = JsonUtils.parseArray(featuresJson, String.class);
        return JsonUtils.toJsonString(features == null ? List.of() : features);
    }

    private List<String> parseFeatures(String featuresJson) {
        if (StrUtil.isBlank(featuresJson)) {
            return List.of();
        }
        List<String> features = JsonUtils.parseArray(featuresJson, String.class);
        return features == null ? new ArrayList<>() : features;
    }

    private String toDurationText(Integer durationMonths) {
        if (durationMonths == null) {
            return "";
        }
        return switch (durationMonths) {
            case 3 -> "季度";
            case 6 -> "半年";
            case 12 -> "年度";
            default -> durationMonths + "个月";
        };
    }
}

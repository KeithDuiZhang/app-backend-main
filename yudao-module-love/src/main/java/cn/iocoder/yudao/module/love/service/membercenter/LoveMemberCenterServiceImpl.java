package cn.iocoder.yudao.module.love.service.membercenter;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.love.controller.app.membercenter.vo.AppLoveMemberCenterGroupRespVO;
import cn.iocoder.yudao.module.love.controller.app.membercenter.vo.AppLoveMemberCenterOverviewRespVO;
import cn.iocoder.yudao.module.love.controller.app.membercenter.vo.AppLoveMemberCenterSkuRespVO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberEntitlementSegmentDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberGroupDO;
import cn.iocoder.yudao.module.love.dal.dataobject.member.LoveMemberSkuDO;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberEntitlementSegmentMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberGroupMapper;
import cn.iocoder.yudao.module.love.dal.mysql.member.LoveMemberSkuMapper;
import cn.iocoder.yudao.module.love.dal.mysql.user.LoveUserMapper;
import cn.iocoder.yudao.module.love.service.memberupgrade.LoveMemberUpgradeService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoveMemberCenterServiceImpl implements LoveMemberCenterService {

    @Resource
    private LoveUserMapper loveUserMapper;
    @Resource
    private LoveMemberGroupMapper loveMemberGroupMapper;
    @Resource
    private LoveMemberSkuMapper loveMemberSkuMapper;
    @Resource
    private LoveMemberEntitlementSegmentMapper loveMemberEntitlementSegmentMapper;
    @Resource
    private LoveMemberUpgradeService loveMemberUpgradeService;

    @Override
    public AppLoveMemberCenterOverviewRespVO getOverview(Long userId, String selectedGroupCode, String selectedDurationCode) {
        LocalDateTime now = LocalDateTime.now();
        List<LoveMemberGroupDO> groups = loveMemberGroupMapper.selectList(new LambdaQueryWrapperX<LoveMemberGroupDO>()
                .eq(LoveMemberGroupDO::getStatus, 0)
                .orderByAsc(LoveMemberGroupDO::getSort));
        List<LoveMemberSkuDO> skus = loveMemberSkuMapper.selectList(new LambdaQueryWrapperX<LoveMemberSkuDO>()
                .eq(LoveMemberSkuDO::getStatus, 0)
                .orderByAsc(LoveMemberSkuDO::getSort));
        Map<Long, List<LoveMemberSkuDO>> skuMap = skus.stream().collect(Collectors.groupingBy(LoveMemberSkuDO::getGroupId));

        AppLoveMemberCenterOverviewRespVO respVO = new AppLoveMemberCenterOverviewRespVO();
        respVO.setGroups(groups.stream().map(group -> toGroupResp(group, skuMap.getOrDefault(group.getId(), List.of()))).toList());

        LoveUserDO user = userId == null ? null : loveUserMapper.selectById(userId);
        List<LoveMemberEntitlementSegmentDO> activeSegments = userId == null ? List.of() : loveMemberEntitlementSegmentMapper.selectList(
                new LambdaQueryWrapperX<LoveMemberEntitlementSegmentDO>()
                        .eq(LoveMemberEntitlementSegmentDO::getUserId, userId)
                        .ge(LoveMemberEntitlementSegmentDO::getExpireTime, now)
                        .orderByDesc(LoveMemberEntitlementSegmentDO::getExpireTime));
        if (user != null && user.getMemberLevel() != null && user.getMemberLevel() > 0 && !activeSegments.isEmpty()) {
            LoveMemberEntitlementSegmentDO latestSegment = activeSegments.get(0);
            AppLoveMemberCenterOverviewRespVO.CurrentMember currentMember = new AppLoveMemberCenterOverviewRespVO.CurrentMember();
            currentMember.setGroupId(latestSegment.getGroupId());
            currentMember.setGroupCode(latestSegment.getGroupCode());
            currentMember.setGroupName(latestSegment.getGroupName());
            currentMember.setMemberLevel(user.getMemberLevel());
            currentMember.setExpireTime(user.getMemberExpireTime());
            currentMember.setRemainingDays((int) Math.max(Duration.between(now, user.getMemberExpireTime()).toDays(), 0));
            respVO.setCurrentMember(currentMember);
        }

        String resolvedGroupCode = StrUtil.blankToDefault(selectedGroupCode, groups.isEmpty() ? "silver" : groups.get(0).getCode());
        String resolvedDurationCode = StrUtil.blankToDefault(selectedDurationCode, "year");
        LoveMemberGroupDO selectedGroup = groups.stream().filter(group -> group.getCode().equals(resolvedGroupCode)).findFirst().orElse(null);
        LoveMemberSkuDO selectedSku = skus.stream()
                .filter(sku -> selectedGroup != null && sku.getGroupId().equals(selectedGroup.getId()))
                .filter(sku -> resolveDurationCode(sku.getDurationType()).equals(resolvedDurationCode))
                .findFirst()
                .orElse(null);

        AppLoveMemberCenterOverviewRespVO.SelectedGroupPreview preview = new AppLoveMemberCenterOverviewRespVO.SelectedGroupPreview();
        preview.setSelectedGroupCode(resolvedGroupCode);
        preview.setSelectedDurationCode(resolvedDurationCode);
        preview.setSelectedSkuId(selectedSku != null ? selectedSku.getId() : null);

        String upgradeState = resolveUpgradeState(respVO.getCurrentMember(), selectedGroup, activeSegments, now);
        respVO.setUpgradeState(upgradeState);
        if ("gold".equals(resolvedGroupCode) && "upgradeable".equals(upgradeState) && selectedSku != null) {
            Integer upgradeAmountFen = loveMemberUpgradeService.calculateUpgradePayAmountFen(selectedSku.getSalePriceFen(), activeSegments, now);
            preview.setActionType("upgrade");
            preview.setPrimaryCtaText("立即补差价升级 ¥" + formatFen(upgradeAmountFen));
            preview.setSecondaryHintText("黄金权益立即生效，到期时间保持不变");
            preview.setPreviewUpgradeAmountFen(upgradeAmountFen);
            respVO.setPurchaseMode("upgrade");
        } else {
            preview.setActionType("buy");
            preview.setPrimaryCtaText(selectedSku == null ? "立即开通" : "立即开通 ¥" + formatFen(selectedSku.getSalePriceFen()));
            preview.setSecondaryHintText(selectedGroup != null && "gold".equals(selectedGroup.getCode()) ? "黄金权益立即生效" : "购买成功后权益立即生效");
            preview.setPreviewUpgradeAmountFen(null);
            respVO.setPurchaseMode("buy");
        }
        respVO.setSelectedGroupPreview(preview);

        if (!"upgradeable".equals(upgradeState)) {
            AppLoveMemberCenterOverviewRespVO.ExceptionAction action = new AppLoveMemberCenterOverviewRespVO.ExceptionAction();
            action.setActionType("redirect");
            action.setTargetGroupCode("gold");
            action.setActionText(switch (upgradeState) {
                case "expired", "remaining_too_short" -> "去开通黄金会员";
                case "already_gold" -> "查看黄金权益";
                default -> "继续查看会员";
            });
            respVO.setExceptionAction(action);
        }
        return respVO;
    }

    private AppLoveMemberCenterGroupRespVO toGroupResp(LoveMemberGroupDO group, List<LoveMemberSkuDO> skus) {
        AppLoveMemberCenterGroupRespVO respVO = new AppLoveMemberCenterGroupRespVO();
        respVO.setGroupId(group.getId());
        respVO.setCode(group.getCode());
        respVO.setName(group.getName());
        respVO.setLevel(group.getLevel());
        respVO.setTheme(group.getTheme());
        respVO.setBenefits(parseBenefits(group.getBenefitsJson()));
        respVO.setSkus(skus.stream().sorted(Comparator.comparing(LoveMemberSkuDO::getSort)).map(this::toSkuResp).toList());
        return respVO;
    }

    private AppLoveMemberCenterSkuRespVO toSkuResp(LoveMemberSkuDO sku) {
        AppLoveMemberCenterSkuRespVO respVO = new AppLoveMemberCenterSkuRespVO();
        respVO.setSkuId(sku.getId());
        respVO.setDurationType(sku.getDurationType());
        respVO.setDurationCode(resolveDurationCode(sku.getDurationType()));
        respVO.setDurationDays(sku.getDurationDays());
        respVO.setOriginalPriceFen(sku.getOriginalPriceFen());
        respVO.setSalePriceFen(sku.getSalePriceFen());
        respVO.setTagText(sku.getTagText());
        respVO.setRecommend(Boolean.TRUE.equals(sku.getRecommend()));
        respVO.setSort(sku.getSort());
        return respVO;
    }

    private String resolveUpgradeState(AppLoveMemberCenterOverviewRespVO.CurrentMember currentMember,
                                       LoveMemberGroupDO selectedGroup,
                                       List<LoveMemberEntitlementSegmentDO> activeSegments,
                                       LocalDateTime now) {
        if (currentMember == null || activeSegments.isEmpty()) {
            return "expired";
        }
        if (selectedGroup == null || !"gold".equals(selectedGroup.getCode())) {
            return "upgradeable";
        }
        if (currentMember.getMemberLevel() != null && currentMember.getMemberLevel() >= selectedGroup.getLevel()) {
            return "already_gold";
        }
        long remainingDays = Duration.between(now, activeSegments.stream()
                .map(LoveMemberEntitlementSegmentDO::getExpireTime)
                .max(LocalDateTime::compareTo)
                .orElse(now)).toDays();
        return remainingDays <= 0 ? "remaining_too_short" : "upgradeable";
    }

    private List<String> parseBenefits(String benefitsJson) {
        if (StrUtil.isBlank(benefitsJson)) {
            return List.of();
        }
        String normalized = benefitsJson.replace("[", "").replace("]", "").replace("\"", "");
        return StrUtil.split(normalized, ',').stream().map(String::trim).filter(StrUtil::isNotBlank).toList();
    }

    private String resolveDurationCode(Integer durationType) {
        return switch (durationType == null ? 0 : durationType) {
            case 1 -> "quarter";
            case 2 -> "half_year";
            case 3 -> "year";
            default -> "custom";
        };
    }

    private String formatFen(Integer amountFen) {
        if (amountFen == null) {
            return "0";
        }
        int yuan = amountFen / 100;
        int cents = Math.abs(amountFen % 100);
        return cents == 0 ? String.valueOf(yuan) : String.format("%d.%02d", yuan, cents);
    }
}

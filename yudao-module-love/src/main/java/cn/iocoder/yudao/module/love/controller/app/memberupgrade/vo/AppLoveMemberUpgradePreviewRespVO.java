package cn.iocoder.yudao.module.love.controller.app.memberupgrade.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 App - 会员升级预览 Response VO")
@Data
public class AppLoveMemberUpgradePreviewRespVO {

    private Boolean canUpgrade;

    private String upgradeState;

    private CurrentMember currentMember;

    private TargetMember targetMember;

    private PriceDetail price;

    private List<String> tips;

    @Data
    public static class CurrentMember {
        private Long groupId;
        private String groupCode;
        private String groupName;
        private Integer memberLevel;
        private Integer durationType;
        private Integer durationDays;
        private LocalDateTime expireTime;
        private Integer remainingDays;
    }

    @Data
    public static class TargetMember {
        private Long groupId;
        private String groupCode;
        private String groupName;
        private Long targetSkuId;
        private Integer durationType;
        private Integer durationDays;
    }

    @Data
    public static class PriceDetail {
        private Integer targetSalePriceFen;
        private Integer currentPaidAmountFen;
        private Integer fullDiffAmountFen;
        private Integer remainingDays;
        private Integer totalDurationDays;
        private Integer upgradeAmountFen;
    }
}

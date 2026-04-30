package cn.iocoder.yudao.module.love.controller.app.membercenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "用户 App - 会员中心总览 Response VO")
@Data
public class AppLoveMemberCenterOverviewRespVO {

    private CurrentMember currentMember;

    private List<AppLoveMemberCenterGroupRespVO> groups;

    private String purchaseMode;

    private String upgradeState;

    private SelectedGroupPreview selectedGroupPreview;

    private ExceptionAction exceptionAction;

    @Data
    public static class CurrentMember {
        private Long groupId;
        private String groupCode;
        private String groupName;
        private Integer memberLevel;
        private LocalDateTime expireTime;
        private Integer remainingDays;
    }

    @Data
    public static class SelectedGroupPreview {
        private String selectedGroupCode;
        private String selectedDurationCode;
        private Long selectedSkuId;
        private String actionType;
        private String primaryCtaText;
        private String secondaryHintText;
        private Integer previewUpgradeAmountFen;
    }

    @Data
    public static class ExceptionAction {
        private String actionType;
        private String actionText;
        private String targetGroupCode;
    }
}

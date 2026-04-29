package cn.iocoder.yudao.module.love.controller.admin.memberpackage.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员套餐分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberPackagePageReqVO extends PageParam {

    @Schema(description = "套餐名称")
    private String name;

    @Schema(description = "会员等级")
    private Integer level;

    @Schema(description = "状态")
    private Integer status;
}

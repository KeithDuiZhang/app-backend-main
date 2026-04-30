package cn.iocoder.yudao.module.love.controller.admin.membersku.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员 SKU 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberSkuPageReqVO extends PageParam {

    @Schema(description = "会员分组编号")
    private Long groupId;

    @Schema(description = "时长类型")
    private Integer durationType;

    @Schema(description = "状态")
    private Integer status;
}

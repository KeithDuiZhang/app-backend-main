package cn.iocoder.yudao.module.love.controller.admin.membergroup.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 会员分组分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class LoveMemberGroupPageReqVO extends PageParam {

    @Schema(description = "分组编码")
    private String code;

    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "状态")
    private Integer status;
}

package cn.iocoder.yudao.module.love.dal.dataobject.match;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 牵线申请日志 DO
 */
@TableName("love_match_apply_log")
@KeySequence("love_match_apply_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMatchApplyLogDO extends BaseDO {

    /**
     * 日志编号
     */
    @TableId
    private Long id;
    /**
     * 申请编号
     */
    private Long applyId;
    /**
     * 变更前状态
     */
    private Integer fromStatus;
    /**
     * 变更后状态
     */
    private Integer toStatus;
    /**
     * 操作人类型
     */
    private Integer operatorType;
    /**
     * 操作人编号
     */
    private Long operatorId;
    /**
     * 备注
     */
    private String remark;

}

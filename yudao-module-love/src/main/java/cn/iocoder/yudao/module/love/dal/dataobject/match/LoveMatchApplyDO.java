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

import java.time.LocalDateTime;

/**
 * 牵线申请 DO
 */
@TableName("love_match_apply")
@KeySequence("love_match_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMatchApplyDO extends BaseDO {

    /**
     * 牵线申请编号
     */
    @TableId
    private Long id;
    /**
     * 发起用户编号
     */
    private Long fromUserId;
    /**
     * 目标用户编号
     */
    private Long toUserId;
    /**
     * 红娘编号
     */
    private Long matchmakerId;
    /**
     * 申请状态
     */
    private Integer status;
    /**
     * 拒绝原因
     */
    private String rejectReason;
    /**
     * 提交时间
     */
    private LocalDateTime submittedAt;
    /**
     * 处理时间
     */
    private LocalDateTime processedAt;
    /**
     * 申请原因
     */
    private String applyReason;
    /**
     * 申请来源
     */
    private Integer sourceType;
    /**
     * 最新日志编号
     */
    private Long latestLogId;

}

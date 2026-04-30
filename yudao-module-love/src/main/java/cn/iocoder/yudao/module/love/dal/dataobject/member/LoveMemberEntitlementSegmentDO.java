package cn.iocoder.yudao.module.love.dal.dataobject.member;

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
 * 婚恋会员权益分段 DO
 */
@TableName("love_member_entitlement_segment")
@KeySequence("love_member_entitlement_segment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMemberEntitlementSegmentDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long sourceOrderId;

    private Integer sourceOrderType;

    private Long groupId;

    private Long skuId;

    private String groupCode;

    private String groupName;

    private Integer durationType;

    private Integer durationDays;

    private Integer paidAmountFen;

    private LocalDateTime startTime;

    private LocalDateTime expireTime;
}

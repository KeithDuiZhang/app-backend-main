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
 * 婚恋会员订单 DO
 */
@TableName("love_member_order")
@KeySequence("love_member_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMemberOrderDO extends BaseDO {

    @TableId
    private Long id;

    private Long userId;

    private Long packageId;

    private Long payOrderId;

    private String orderNo;

    private String packageName;

    private Integer memberLevel;

    private Integer priceFen;

    private Integer durationMonths;

    private Integer status;

    private LocalDateTime payTime;

    private LocalDateTime memberStartTime;

    private LocalDateTime memberEndTime;
}

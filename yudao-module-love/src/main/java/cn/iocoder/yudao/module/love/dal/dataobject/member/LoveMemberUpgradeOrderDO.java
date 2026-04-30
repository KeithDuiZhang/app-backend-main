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
 * 婚恋会员升级订单 DO
 */
@TableName("love_member_upgrade_order")
@KeySequence("love_member_upgrade_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMemberUpgradeOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String orderNo;

    private Long userId;

    private Long fromGroupId;

    private Long toGroupId;

    private Long targetSkuId;

    private Integer remainingDays;

    private Integer fullDiffAmountFen;

    private Integer upgradeAmountFen;

    private Long payOrderId;

    private Integer status;

    private LocalDateTime payTime;
}

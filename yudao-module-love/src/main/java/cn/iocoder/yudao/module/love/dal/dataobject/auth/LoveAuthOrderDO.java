package cn.iocoder.yudao.module.love.dal.dataobject.auth;

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
 * 婚恋认证订单 DO
 */
@TableName("love_auth_order")
@KeySequence("love_auth_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveAuthOrderDO extends BaseDO {

    /**
     * 认证订单编号
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 支付订单编号
     */
    private Long payOrderId;
    /**
     * 业务订单号
     */
    private String orderNo;
    /**
     * 支付金额(分)
     */
    private Integer amount;
    /**
     * 认证状态
     */
    private Integer status;
    /**
     * 认证结果
     */
    private String verifiedResult;
    /**
     * 支付时间
     */
    private LocalDateTime payTime;
    /**
     * 认证完成时间
     */
    private LocalDateTime verifiedTime;

}

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

/**
 * 婚恋会员 SKU DO
 */
@TableName("love_member_sku")
@KeySequence("love_member_sku_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMemberSkuDO extends BaseDO {

    @TableId
    private Long id;

    private Long groupId;

    private Integer durationType;

    private Integer durationDays;

    private Integer salePriceFen;

    private Integer originalPriceFen;

    private String tagText;

    private Boolean recommend;

    private Integer sort;

    private Integer status;
}

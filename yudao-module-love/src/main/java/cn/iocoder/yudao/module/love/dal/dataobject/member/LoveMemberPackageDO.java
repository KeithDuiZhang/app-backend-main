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
 * 婚恋会员套餐 DO
 */
@TableName("love_member_package")
@KeySequence("love_member_package_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMemberPackageDO extends BaseDO {

    @TableId
    private Long id;

    private String name;

    private Integer level;

    private Integer priceFen;

    private Integer durationMonths;

    private String description;

    private String featuresJson;

    private String theme;

    private Boolean popular;

    private Integer sort;

    private Integer status;
}

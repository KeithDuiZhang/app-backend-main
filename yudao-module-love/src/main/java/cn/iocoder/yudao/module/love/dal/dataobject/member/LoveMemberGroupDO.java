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
 * 婚恋会员分组 DO
 */
@TableName("love_member_group")
@KeySequence("love_member_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMemberGroupDO extends BaseDO {

    @TableId
    private Long id;

    private String code;

    private String name;

    private Integer level;

    private String theme;

    private String benefitsJson;

    private Integer sort;

    private Integer status;
}

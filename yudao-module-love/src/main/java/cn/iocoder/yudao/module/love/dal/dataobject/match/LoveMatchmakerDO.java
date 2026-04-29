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
 * 红娘 DO
 */
@TableName("love_matchmaker")
@KeySequence("love_matchmaker_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveMatchmakerDO extends BaseDO {

    /**
     * 红娘编号
     */
    @TableId
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 简介
     */
    private String introduction;
    /**
     * 联系电话
     */
    private String mobile;
    /**
     * 微信号
     */
    private String wechatNo;
    /**
     * 排序值
     */
    private Integer sort;
    /**
     * 是否默认红娘
     */
    private Boolean isDefault;
    /**
     * 状态
     */
    private Integer status;

}

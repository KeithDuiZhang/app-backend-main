package cn.iocoder.yudao.module.love.dal.dataobject.user;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 婚恋用户资料 DO
 */
@TableName("love_user_profile")
@KeySequence("love_user_profile_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveUserProfileDO extends BaseDO {

    /**
     * 资料编号
     */
    @TableId
    private Long id;
    /**
     * 用户编号
     */
    private Long userId;
    /**
     * 真实姓名
     */
    private String realName;
    /**
     * 性别
     */
    private Integer gender;
    /**
     * 生日
     */
    private LocalDate birthday;
    /**
     * 城市编码
     */
    private String cityCode;
    /**
     * 城市名称
     */
    private String cityName;
    /**
     * 婚姻状态
     */
    private Integer maritalStatus;
    /**
     * 身高(cm)
     */
    private Integer heightCm;
    /**
     * 体重(kg)
     */
    private Integer weightKg;
    /**
     * 职业
     */
    private String profession;
    /**
     * 学历
     */
    private String education;
    /**
     * 收入描述
     */
    private String incomeDesc;
    /**
     * 照片列表
     */
    private String photos;
    /**
     * 个人介绍
     */
    private String bio;
    /**
     * 标签列表
     */
    private String tags;
    /**
     * 择偶要求
     */
    private String partnerPreference;
    /**
     * 资料是否公开
     */
    private Boolean profilePublic;
    /**
     * 资料完成度
     */
    private Integer completionRate;

}

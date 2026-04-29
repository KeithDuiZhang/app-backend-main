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

import java.time.LocalDateTime;

/**
 * 婚恋用户 DO
 */
@TableName("love_user")
@KeySequence("love_user_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoveUserDO extends BaseDO {

    /**
     * 用户编号
     */
    @TableId
    private Long id;
    /**
     * 社交用户编号
     */
    private Long socialUserId;
    /**
     * OpenID
     */
    private String openid;
    /**
     * UnionID
     */
    private String unionid;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 手机号
     */
    private String mobile;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 认证状态
     */
    private Integer authStatus;
    /**
     * 认证完成时间
     */
    private LocalDateTime certifiedAt;
    /**
     * 会员等级
     */
    private Integer memberLevel;
    /**
     * 会员到期时间
     */
    private LocalDateTime memberExpireTime;
    /**
     * 免费牵线额度
     */
    private Integer freeMatchQuota;
    /**
     * 最近额度重置时间
     */
    private LocalDateTime lastQuotaResetAt;
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

}

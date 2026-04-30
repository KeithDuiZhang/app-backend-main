package cn.iocoder.yudao.module.love.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {

    ErrorCode LOVE_MATCHMAKER_NOT_EXISTS = new ErrorCode(1_021_000_000, "红娘不存在");
    ErrorCode LOVE_MATCHMAKER_DEFAULT_NOT_EXISTS = new ErrorCode(1_021_000_001, "默认红娘不存在");

    ErrorCode LOVE_MATCH_APPLY_NOT_EXISTS = new ErrorCode(1_021_001_000, "牵线申请不存在");
    ErrorCode LOVE_MATCH_APPLY_SELF_NOT_ALLOWED = new ErrorCode(1_021_001_001, "不能申请自己");
    ErrorCode LOVE_MATCH_APPLY_DUPLICATE = new ErrorCode(1_021_001_002, "请勿重复申请同一用户");
    ErrorCode LOVE_MATCH_APPLY_AUTH_REQUIRED = new ErrorCode(1_021_001_003, "请先完成认证后再申请牵线");
    ErrorCode LOVE_MATCH_APPLY_PROFILE_REQUIRED = new ErrorCode(1_021_001_004, "请先完善基础资料后再申请牵线");
    ErrorCode LOVE_MATCH_APPLY_QUOTA_EMPTY = new ErrorCode(1_021_001_005, "本月免费牵线次数已用完");
    ErrorCode LOVE_MATCH_APPLY_STATUS_INVALID = new ErrorCode(1_021_001_006, "当前状态不允许执行该操作");

    ErrorCode LOVE_MEMBER_PACKAGE_NOT_EXISTS = new ErrorCode(1_021_002_000, "会员套餐不存在");
    ErrorCode LOVE_MEMBER_PACKAGE_NAME_EXISTS = new ErrorCode(1_021_002_001, "会员套餐名称已存在");
    ErrorCode LOVE_MEMBER_PACKAGE_DISABLED = new ErrorCode(1_021_002_002, "会员套餐已停用");
    ErrorCode LOVE_MEMBER_GROUP_NOT_EXISTS = new ErrorCode(1_021_002_003, "会员分组不存在");
    ErrorCode LOVE_MEMBER_SKU_NOT_EXISTS = new ErrorCode(1_021_002_004, "会员 SKU 不存在");
    ErrorCode LOVE_MEMBER_UPGRADE_NOT_ALLOWED = new ErrorCode(1_021_002_005, "当前状态不允许升级");

    ErrorCode LOVE_MEMBER_ORDER_NOT_EXISTS = new ErrorCode(1_021_003_000, "会员订单不存在");
    ErrorCode LOVE_MEMBER_UPGRADE_ORDER_NOT_EXISTS = new ErrorCode(1_021_003_001, "会员升级订单不存在");
}

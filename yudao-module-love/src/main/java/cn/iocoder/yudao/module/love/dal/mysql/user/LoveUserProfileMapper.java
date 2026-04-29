package cn.iocoder.yudao.module.love.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserProfileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 婚恋用户资料 Mapper
 */
@Mapper
public interface LoveUserProfileMapper extends BaseMapperX<LoveUserProfileDO> {
}

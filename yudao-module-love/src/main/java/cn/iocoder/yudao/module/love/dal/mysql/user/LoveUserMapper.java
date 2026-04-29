package cn.iocoder.yudao.module.love.dal.mysql.user;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.love.dal.dataobject.user.LoveUserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 婚恋用户 Mapper
 */
@Mapper
public interface LoveUserMapper extends BaseMapperX<LoveUserDO> {
}

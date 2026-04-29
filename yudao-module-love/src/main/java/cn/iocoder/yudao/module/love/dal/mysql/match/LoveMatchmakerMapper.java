package cn.iocoder.yudao.module.love.dal.mysql.match;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchmakerDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 红娘 Mapper
 */
@Mapper
public interface LoveMatchmakerMapper extends BaseMapperX<LoveMatchmakerDO> {

    /**
     * 查询默认红娘
     *
     * @return 默认红娘
     */
    LoveMatchmakerDO selectDefaultMatchmaker();

}

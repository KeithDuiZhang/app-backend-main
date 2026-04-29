package cn.iocoder.yudao.module.love.dal.mysql.auth;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.love.dal.dataobject.auth.LoveAuthOrderDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 婚恋认证订单 Mapper
 */
@Mapper
public interface LoveAuthOrderMapper extends BaseMapperX<LoveAuthOrderDO> {

    default LoveAuthOrderDO selectLatestByUserId(Long userId) {
        return CollUtil.getFirst(selectList(new LambdaQueryWrapperX<LoveAuthOrderDO>()
                .eq(LoveAuthOrderDO::getUserId, userId)
                .orderByDesc(LoveAuthOrderDO::getId)));
    }
}

package cn.iocoder.yudao.module.love.dal.mysql.match;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.love.dal.dataobject.match.LoveMatchApplyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 牵线申请 Mapper
 */
@Mapper
public interface LoveMatchApplyMapper extends BaseMapperX<LoveMatchApplyDO> {

    /**
     * 查询重复申请数量
     *
     * @param fromUserId 发起用户编号
     * @param toUserId 目标用户编号
     * @return 数量
     */
    Long selectDuplicateCount(@Param("fromUserId") Long fromUserId, @Param("toUserId") Long toUserId);

}

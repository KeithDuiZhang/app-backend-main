package cn.iocoder.yudao.module.love.service.recommend;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserPageReqVO;
import cn.iocoder.yudao.module.love.controller.app.recommend.vo.AppLoveRecommendUserRespVO;

public interface LoveRecommendUserService {

    PageResult<AppLoveRecommendUserRespVO> getRecommendPage(AppLoveRecommendUserPageReqVO reqVO);

    AppLoveRecommendUserRespVO getRecommendUser(Long userId);
}

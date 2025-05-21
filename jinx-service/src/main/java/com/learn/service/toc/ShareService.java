package com.learn.service.toc;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.share.ShareContentResponse;

/**
 * 分享服务接口
 */
public interface ShareService {
    
    /**
     * 获取内容分享信息
     *
     * @param type 内容类型：course-课程，train-培训，map-学习地图
     * @param id 内容ID
     * @return 分享信息响应
     */
    ApiResponse<ShareContentResponse> getShareContent(String type, Integer id);
}

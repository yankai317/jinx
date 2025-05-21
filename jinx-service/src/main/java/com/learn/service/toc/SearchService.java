package com.learn.service.toc;

import com.learn.dto.toc.search.SearchContentRequest;
import com.learn.dto.toc.search.SearchContentResponse;

/**
 * 搜索服务接口
 */
public interface SearchService {
    /**
     * 搜索课程、培训和学习地图内容
     *
     * @param userId 用户ID
     * @param request 搜索请求参数
     * @return 搜索结果响应
     */
    SearchContentResponse searchContent(Long userId, SearchContentRequest request);
}

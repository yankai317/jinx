package com.learn.dto.toc.search;

import lombok.Data;

/**
 * 搜索内容请求
 */
@Data
public class SearchContentRequest {
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 内容类型：all-全部，course-课程，train-培训，map-学习地图，默认全部
     */
    private String type = "all";
    
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}

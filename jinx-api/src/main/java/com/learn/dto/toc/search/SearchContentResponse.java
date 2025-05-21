package com.learn.dto.toc.search;

import lombok.Data;
import java.util.List;

/**
 * 搜索内容响应
 */
@Data
public class SearchContentResponse {
    /**
     * 总记录数
     */
    private Integer total;
    
    /**
     * 搜索结果列表
     */
    private List<SearchItemDTO> list;
    
    /**
     * 当前页码
     */
    private Integer pageNum;
    
    /**
     * 每页条数
     */
    private Integer pageSize;
    
    /**
     * 总页数
     */
    private Integer totalPages;
}

package com.learn.service.dto.base;

import java.util.List;

/**
 * 基础请求对象
 */
public class BaseRequest {
    private Integer pageSize;
    private Integer pageNum;
    private String keyword;
    private Long id;
    private List<Long> ids;
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public Integer getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }
    
    public String getKeyword() {
        return keyword;
    }
    
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public List<Long> getIds() {
        return ids;
    }
    
    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
} 
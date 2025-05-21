package com.learn.dto.train;

import com.learn.dto.common.RangeBaseRequest;
import lombok.Data;

import java.util.List;

/**
 * 更新培训请求类
 */
@Data
public class TrainUpdateRequest extends RangeBaseRequest {
    /**
     * 培训ID
     */
    private Long id;
    
    /**
     * 培训名称
     */
    private String title;
    
    /**
     * 封面图URL
     */
    private String cover;
    
    /**
     * 培训简介
     */
    private String introduction;
    
    /**
     * 学分
     */
    private Integer credit;
    
    /**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 是否允许评论
     */
    private Boolean allowComment;
    
    /**
     * 证书ID
     */
    private Long certificateId;
    
    /**
     * 状态：draft, published
     */
    private String status;
    
    /**
     * 是否可引用
     */
    private Boolean ifIsCitable;
    
    /**
     * 培训内容列表
     */
    private List<TrainContentRequest> contents;
    
    /**
     * 要删除的内容关联ID列表
     */
    private List<Long> deleteContentIds;

    
    /**
     * 培训内容请求
     */
    @Data
    public static class TrainContentRequest {
        /**
         * 内容关联ID，新增内容不传
         */
        private Long id;
        
        /**
         * 内容类型：CONTENT, EXAM, ASSIGNMENT, SURVEY
         */
        private String type;
        
        /**
         * 内容ID
         */
        private Long contentId;
        
        /**
         * 是否必修
         */
        private Boolean isRequired;
        
        /**
         * 排序序号
         */
        private Integer sortOrder;
    }
}

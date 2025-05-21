package com.learn.dto.train;

import com.learn.dto.course.sub.CollaboratorsDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 培训详情DTO
 */
@Data
public class TrainDetailDTO {
    /**
     * 培训ID
     */
    private Long id;/**
     * 培训名称
     */
    private String title;
    
    /**
     * 封面图URL
     */
    private String cover;/**
     * 培训简介
     */
    private String introduction;/**
     * 学分
     */
    private Integer credit;/**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 分类名称列表
     */
    private List<String> categoryNames;
    
    /**
     * 是否允许评论
     */
    private Boolean allowComment;/**
     * 证书ID
     */
    private Long certificateId;
    
    /**
     * 证书名称
     */
    private String certificateName;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;/**
     * 创建时间
     */
    private Date gmtCreate;
    
    /**
     * 培训内容列表
     */
    private List<TrainContentDTO> contents;/**
     * 可见范围信息
     */
    private VisibilityDTO visibility;
    
    /**
     * 协同管理信息
     */
    private CollaboratorsDTO collaborators;
    
    /**
     * 是否可引用
     */
    private Boolean ifIsCitable;
    /**
     * 培训内容DTO
     */
    @Data
    public static class TrainContentDTO {
        /**
         * 内容关联ID
         */
        private Long id;
        
        /**
         * 内容类型
         */
        private String type;
        
        /**
         * 内容ID
         */
        private Long contentId;
        
        /**
         * 内容标题
         */
        private String title;
        
        /**
         * 内容子类型(课程类型)
         */
        private String contentType;
        
        /**
         * 内容URL
         */
        private String contentUrl;
        
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

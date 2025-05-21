package com.learn.dto.map;

import com.learn.dto.course.sub.CollaboratorsDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 学习地图详情DTO
 */
@Data
public class LearningMapDetailDTO {
    /**
     * 地图ID
     */
    private Long id;
    
    /**
     * 地图名称
     */
    private String name;/**
     * 封面图URL
     */
    private String cover;
    
    /**
     * 地图简介
     */
    private String introduction;
    
    /**
     * 学分规则：0-整体发放 1-按阶段发放
     */
    private Integer creditRule;
    
    /**
     * 必修学分
     */
    private Integer requiredCredit;
    
    /**
     * 选修学分
     */
    private Integer electiveCredit;/**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 分类名称列表
     */
    private List<String> categoryNames;
    
    /**
     * 证书规则：0-不发放 1-整体发放 2-按阶段发放
     */
    private Integer certificateRule;/**
     * 证书ID
     */
    private Long certificateId;
    
    /**
     * 证书名称
     */
    private String certificateName;
    
    /**
     * 是否创建钉钉群：0-否 1-是
     */
    private Integer dingtalkGroup;/**
     *钉钉群ID
     */
    private String dingtalkGroupId;
    
    /**
     * 开放开始时间
     */
    private String startTime;
    
    /**
     * 开放结束时间
     */
    private String endTime;/**
     * 解锁方式：0-按阶段和任务 1-按阶段 2-自由模式
     */
    private Integer unlockMode;
    
    /**
     * 主题
     */
    private String theme;
    
    /**
     * 创建人ID
     */
    private Long creatorId;
    
    /**
     * 创建人名称
     */
    private String creatorName;
    
    /**
     * 阶段列表
     */
    private List<StageDTO> stages;
    
    /**
     * 可见范围信息
     */
    private VisibilityDTO visibility;/**
     * 协同管理信息
     */
    private CollaboratorsDTO collaborators;/**
     *阶段DTO
     */
    @Data
    public static class StageDTO {
        /**
         * 阶段ID
         */
        private Long id;
        
        /**
         * 阶段名称
         */
        private String name;
        /**
         * 阶段顺序
         */
        private Integer stageOrder;
        /**
         * 开放类型：0-不设置 1-固定时间 2-学习期限
         */
        private Integer openType;
        
        /**
         * 开放开始时间
         */
        private String startTime;
        
        /**
         * 开放结束时间
         */
        private String endTime;
        
        /**
         * 学习期限(天)
         */
        private Integer durationDays;
        
        /**
         * 阶段学分
         */
        private Integer credit;
        
        /**
         * 证书ID
         */
        private Long certificateId;
        
        /**
         * 证书名称
         */
        private String certificateName;
        /**
         * 任务列表
         */
        private List<TaskDTO> tasks;
    }
    
    /**
     * 任务DTO
     */
    @Data
    public static class TaskDTO {
        /**
         * 任务关联ID
         */
        private Long id;
        
        /**
         * 任务类型：CONTENT, EXAM, ASSIGNMENT, SURVEY
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

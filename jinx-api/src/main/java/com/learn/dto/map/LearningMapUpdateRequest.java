package com.learn.dto.map;

import com.learn.dto.common.RangeBaseRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 更新学习地图请求
 */
@Data
public class LearningMapUpdateRequest extends RangeBaseRequest {
    /**
     * 地图ID
     */
    private Long id;
    
    /**
     * 地图名称
     */
    private String name;
    
    /**
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
    private Integer electiveCredit;
    
    /**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 证书规则：0-不发放 1-整体发放 2-按阶段发放
     */
    private Integer certificateRule;
    
    /**
     * 证书ID，certificateRule为1时必填
     */
    private Long certificateId;
    
    /**
     * 是否创建钉钉群：0-否 1-是
     */
    private Integer dingtalkGroup;
    
    /**
     * 开放开始时间
     */
    private String startTime;
    
    /**
     * 开放结束时间
     */
    private String endTime;
    
    /**
     * 解锁方式：0-按阶段和任务 1-按阶段 2-自由模式
     */
    private Integer unlockMode;
    
    /**
     * 主题：business, tech, farm, chinese, list
     */
    private String theme;
    
    /**
     * 阶段列表
     */
    private List<StageRequest> stages;
    
    /**
     * 要删除的阶段ID列表
     */
    private List<Long> deleteStageIds;
    
    /**
     * 要删除的任务关联ID列表
     */
    private List<Long> deleteTaskIds;
    
    /**
     * 阶段请求
     */
    @Data
    public static class StageRequest {
        /**
         * 阶段ID，新增阶段不传
         */
        private Long id;
        
        /**
         * 阶段名称
         */
        private String name;/**
         * 阶段顺序
         */
        private Integer stageOrder;
        
        /**
         * 开放类型：0-不设置 1-固定时间 2-学习期限
         */
        private Integer openType;
        
        /**
         * 开放开始时间，openType为1时必填
         */
        private String startTime;
        
        /**
         * 开放结束时间，openType为1时必填
         */
        private String endTime;
        
        /**
         * 学习期限(天)，openType为2时必填
         */
        private Integer durationDays;
        
        /**
         * 阶段学分，creditRule为1时必填
         */
        private Integer credit;
        
        /**
         * 证书ID，certificateRule为2时必填
         */
        private Long certificateId;
        
        /**
         * 任务列表
         */
        private List<TaskRequest> tasks;
    }
    
    /**
     * 任务请求
     */
    @Data
    public static class TaskRequest {
        /**
         * 任务关联ID，新增任务不传
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
         * 是否必修
         */
        private Boolean isRequired;
        
        /**
         * 排序序号
         */
        private Integer sortOrder;
    }
}

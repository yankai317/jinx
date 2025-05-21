package com.learn.dto.map;

import com.learn.dto.common.RangeBaseRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建学习地图请求
 */
@Data
public class LearningMapCreateRequest extends RangeBaseRequest {
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
     * 学分规则：0-整体发放 1-按阶段发放，默认0
     */
    private Integer creditRule;
    
    /**
     * 必修学分，默认0
     */
    private Integer requiredCredit;
    
    /**
     * 选修学分，默认0
     */
    private Integer electiveCredit;
    
    /**
     * 分类ID列表
     */
    private String categoryIds;
    
    /**
     * 证书规则：0-不发放 1-整体发放 2-按阶段发放，默认0
     */
    private Integer certificateRule;
    
    /**
     * 证书ID，certificateRule为1时必填
     */
    private Long certificateId;
    
    /**
     * 是否创建钉钉群：0-否 1-是，默认0
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
     * 解锁方式：0-按阶段和任务 1-按阶段 2-自由模式，默认0
     */
    private Integer unlockMode;
    
    /**
     * 主题：business, tech, farm, chinese, list，默认business
     */
    private String theme;
    
    /**
     * 阶段列表
     */
    private List<StageRequest> stages;
    
    /**
     * 阶段请求
     */
    @Data
    public static class StageRequest {
        /**
         * 阶段名称
         */
        private String name;
        
        /**
         * 阶段顺序
         */
        private Integer stageOrder;
        
        /**
         * 开放类型：0-不设置 1-固定时间 2-学习期限，默认0
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
         * 任务类型：CONTENT, EXAM, ASSIGNMENT, SURVEY
         */
        private String type;

        /**
         * 业务类型：
         *
         *
         */
        private String subType;
        
        /**
         * 内容ID
         */
        private Long contentId;
        
        /**
         * 是否必修，默认true
         */
        private Boolean isRequired;
        
        /**
         * 排序序号，默认0
         */
        private Integer sortOrder;
    }

}

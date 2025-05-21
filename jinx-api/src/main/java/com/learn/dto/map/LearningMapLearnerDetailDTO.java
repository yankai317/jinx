package com.learn.dto.map;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 学习地图学员学习详情DTO
 */
@Data
public class LearningMapLearnerDetailDTO {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户姓名
     */
    private String userName;/**
     * 部门
     */
    private String department;/**
     * 学习来源：ASSIGN-指派 SELF-自学
     */
    private String source;
    
    /**
     * 完成状态：0-学习中 1-已完成
     */
    private String status;

    /**
     * 学习时长(分钟)
     */
    private Integer studyDuration;
    
    /**
     * 当前学习阶段ID
     */
    private Long currentStageId;
    
    /**
     * 当前学习阶段名称
     */
    private String currentStageName;
    
    /**
     * 已完成阶段数
     */
    private Integer completedStageCount;
    
    /**
     * 已完成必修任务数
     */
    private Integer completedRequiredTaskCount;
    
    /**
     * 已完成选修任务数
     */
    private Integer completedElectiveTaskCount;
    
    /**
     * 已获得学分
     */
    private Integer earnedCredit;
    
    /**
     * 是否已发放证书
     */
    private Boolean certificateIssued;
    
    /**
     * 指派时间
     */
    private String assignTime;
    
    /**
     * 开始学习时间
     */
    private String startTime;
    
    /**
     * 截止时间
     */
    private String deadline;/**
     * 完成时间
     */
    private String completionTime;/**
     * 最后学习时间
     */
    private String lastStudyTime;
    
    /**
     * 阶段学习记录
     */
    private List<StageRecord> stageRecords;/**
     * 阶段学习记录
     */
    @Data
    public static class StageRecord {
        /**
         * 阶段ID
         */
        private Long stageId;
        
        /**
         * 阶段名称
         */
        private String stageName;
        
        /**
         * 阶段顺序
         */
        private Integer stageOrder;

        /**
         * @See LearningStatus
         */
        private String status;/**
         *阶段学分
         */
        private Integer credit;
        
        /**
         * 证书ID
         */
        private Long certificateId;
        /**
         * 任务学习记录
         */
        private List<TaskRecord> taskRecords;
    }
    
    /**
     * 任务学习记录
     */
    @Data
    public static class TaskRecord {
        /**
         * 任务ID
         */
        private Long taskId;
        
        /**
         * 任务类型
         */
        private String type;
        
        /**
         * 任务标题
         */
        private String title;
        
        /**
         * 完成状态
         * @see com.learn.constants.LearningStatus
         */
        private String status;
        
        /**
         * 学习时长(分钟)
         */
        private Integer studyDuration;/**
         * 学习进度(百分比)
         */
        private Integer progress;
        
        /**
         * 得分(考试/作业)
         */
        private Integer score;

        /**
         * 通过状态：0-未通过 1-已通过
         */
        private Integer passStatus;
        
        /**
         * 开始学习时间
         */
        private String startTime;/**
         * 最后学习时间
         */
        private String lastStudyTime;
        
        /**
         * 完成时间
         */
        private String completionTime;
    }
}

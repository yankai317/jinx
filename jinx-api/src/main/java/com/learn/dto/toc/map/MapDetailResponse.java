package com.learn.dto.toc.map;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学习地图详情响应DTO
 */
@Data
public class MapDetailResponse {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 学习地图详情数据
     */
    private MapDetailData data;
    
    /**
     * 学习地图详情数据
     */
    @Data
    public static class MapDetailData {
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
         * 证书规则：0-不发放 1-整体发放 2-按阶段发放
         */
        private Integer certificateRule;
        
        /**
         * 证书信息
         */
        private CertificateInfo certificate;
        
        /**
         * 解锁方式：0-按阶段和任务 1-按阶段 2-自由模式
         */
        private Integer unlockMode;
        
        /**
         * 主题
         */
        private String theme;
        
        /**
         * 用户学习进度
         */
        private UserProgress userProgress;
        
        /**
         * 阶段列表
         */
        private List<StageInfo> stages;
        
        /**
         * 学习人员信息
         */
        private LearnersInfo learners;
    }
    
    /**
     * 证书信息
     */
    @Data
    public static class CertificateInfo {
        /**
         * 证书ID
         */
        private Long id;
        
        /**
         * 证书名称
         */
        private String name;
    }
    
    /**
     * 用户学习进度
     */
    @Data
    public static class UserProgress {
        /**
         * 学习状态：learning-学习中 completed-已完成
         */
        private String status;
        
        /**
         * 学习进度百分比
         */
        private Integer progress;
        
        /**
         * 学习时长(分钟)
         */
        private Integer studyDuration;
        
        /**
         * 当前学习阶段ID
         */
        private Long currentStageId;
        
        /**
         * 已完成阶段数
         */
        private Integer completedStageCount;
        
        /**
         * 已获得学分
         */
        private BigDecimal earnedCredit;
    }
    
    /**
     * 阶段信息
     */
    @Data
    public static class StageInfo {
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
         * 阶段学分
         */
        private Integer credit;
        
        /**
         * 阶段状态：locked-未解锁 learning-学习中 completed-已完成
         */
        private String status;
        
        /**
         * 任务列表
         */
        private List<TaskInfo> tasks;
    }
    
    /**
     * 任务信息
     */
    @Data
    public static class TaskInfo {
        /**
         * 任务ID
         */
        private Long id;
        
        /**
         * 任务标题
         */
        private String title;
        
        /**
         * 任务类型：course-课程 exam-考试 assignment-作业 survey-调研
         */
        private String type;
        
        /**
         * 是否必修：true-必修 false-选修
         */
        private Boolean isRequired;
        
        /**
         * 任务状态：locked-未解锁 learning-学习中 completed-已完成
         */
        private String status;
        
        /**
         * 学习进度百分比
         */
        private Integer progress;
    }
    
    /**
     * 学习人员信息
     */
    @Data
    public static class LearnersInfo {
        /**
         * 总学习人数
         */
        private Integer total;
        
        /**
         * 已完成人数
         */
        private Integer completed;
        
        /**
         * 学习中人数
         */
        private Integer learning;
        
        /**
         * 未开始人数
         */
        private Integer notStart;
        
        /**
         * 已完成学习人员列表
         */
        private List<LearnerInfo> completedList;
        
        /**
         * 学习中人员列表
         */
        private List<LearnerInfo> learningList;
        
        /**
         * 未开始人员列表
         */
        private List<LearnerInfo> notStartList;
        /**
         * 学习人员列表（兼容旧版本）
         */
        private List<LearnerInfo> list;
    }
    
    /**
     * 学习人员信息
     */
    @Data
    public static class LearnerInfo {
        /**
         * 用户ID
         */
        private Long userId;
        
        /**
         * 用户昵称
         */
        private String nickname;
        
        /**
         * 用户头像
         */
        private String avatar;
        
        /**
         * 部门
         */
        private String department;
        
        /**
         * 学习状态：learning-学习中 completed-已完成
         */
        private String status;
    }
}

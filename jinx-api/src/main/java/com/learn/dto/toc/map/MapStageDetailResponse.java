package com.learn.dto.toc.map;

import com.learn.constants.LearningStatus;
import com.learn.constants.StageStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学习地图阶段详情响应DTO
 */
@Data
public class MapStageDetailResponse {
    /**
     * 状态码
     */
    private Integer code;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 阶段详情数据
     */
    private StageDetailData data;
    
    /**
     * 阶段详情数据
     */
    @Data
    public static class StageDetailData {
        /**
         * 阶段ID
         */
        private Long id;
        
        /**
         * 学习地图ID
         */
        private Long mapId;
        
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
         * 开放类型：0-不设置开放时间 1-设置固定开放时间 2-设置学员学习期限
         */
        private Integer openType;
        
        /**
         * 用户学习进度
         */
        private UserProgress userProgress;
        
        /**
         * 任务列表
         */
        private List<TaskInfo> tasks;
    }
    
    /**
     * 用户学习进度
     */
    @Data
    public static class UserProgress {
        /**
         * 已完成任务数
         */
        private Integer completedTaskCount;
        
        /**
         * 总任务数
         */
        private Integer totalTaskCount;
        
        /**
         * 学习进度百分比
         */
        private Integer progress;
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
         * 内容类型：video-视频 document-文档 article-文章
         */
        private String contentType;
        
        /**
         * 封面图片URL
         */
        private String coverImage;
        
        /**
         * 是否必修：1-必修 0-选修
         */
        private Integer isRequired;
        
        /**
         * 任务状态：locked-未解锁 learning-学习中 completed-已完成
         */
        private String status;
        
        /**
         * 学习进度百分比
         */
        private Integer progress;

        public void setStatusAndProgress(String learningRecordStatus, Integer progress) {
            if (learningRecordStatus == null) {
                // 未开始学习
                this.setStatus(StageStatus.LOCKED);
                this.setProgress(0);
            } else {
                // 根据状态设置
                switch (learningRecordStatus) {
                    case LearningStatus.NOT_STARTED:
                        this.setStatus(StageStatus.LOCKED);
                        this.setProgress(0);
                        break;
                    case LearningStatus.LEARNING:
                        this.setStatus(StageStatus.LEARNING);
                        this.setProgress(progress);
                        break;
                    case LearningStatus.COMPLETED:
                        this.setStatus(StageStatus.COMPLETED);
                        this.setProgress(100);
                        break;
                    default:
                        this.setStatus(StageStatus.LOCKED);
                        this.setProgress(0);
                }
            }
        }
    }
}

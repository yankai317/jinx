package com.learn.dto.train;

import lombok.Data;
import java.util.List;

/**
 * 培训学员学习详情DTO
 */
@Data
public class TrainLearnerDetailDTO {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 用户姓名
     */
    private String userName;
    
    /**
     * 部门
     */
    private String department;
    
    /**
     * 学习来源
     */
    private String source;
    
    /**
     * 完成状态：0-学习中 1-已完成
     */
    private String status;

    /**
     * 学习时长(分钟)
     */
    private Integer studyDuration;/**
     * 已完成必学任务数
     */
    private Integer requiredTaskFinished;/**
     * 必学任务总数
     */
    private Integer requiredTaskTotal;
    
    /**
     * 指派时间
     */
    private String assignTime;/**
     * 截止时间
     */
    private String deadline;
    
    /**
     * 完成时间
     */
    private String completionTime;
    
    /**
     * 学习记录列表
     */
    private List<RecordDTO> records;
    
    /**
     * 学习记录DTO
     */
    @Data
    public static class RecordDTO {
        /**
         * 内容ID
         */
        private Long contentId;/**
         * 内容类型
         */
        private String contentType;
        
        /**
         * 内容标题
         */
        private String title;
        
        /**
         * 完成状态：0-学习中 1-按时完成 2-逾期完成 -1-未参加
         */
        private String status;
        
        /**
         * 学习时长(分钟)
         */
        private Integer studyDuration;
        
        /**
         * 得分(考试/作业)
         */
        private Integer score;
        
        /**
         * 最后学习时间
         */
        private String lastStudyTime;
        /**
         * 完成时间
         */
        private String completionTime;
    }
}

package com.learn.dto.toc.train;

import com.learn.constants.LearningStatus;
import lombok.Data;

import java.util.List;

/**
 * 培训详情响应DTO
 */
@Data
public class TrainDetailResponse {
    /**
     * 培训ID
     */
    private Long id;

    /**
     * 培训名称
     */
    private String name;

    /**
     * 封面图片地址
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
     * 证书信息
     */
    private CertificateInfo certificate;

    /**
     * 用户学习进度
     */
    private UserProgress userProgress;

    /**
     * 培训内容列表
     */
    private List<ContentItem> contents;

    /**
     * 学习人员信息
     */
    private LearnerInfo learners;

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
         * 学习状态：learning-学习中，completed-已完成
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
         * 已完成必学任务数
         */
        private Integer requiredTaskFinished;

        /**
         * 必学任务总数
         */
        private Integer requiredTaskTotal;
    }

    /**
     * 培训内容项
     */
    @Data
    public static class ContentItem {
        /**
         * 内容关联ID
         */
        private Long id;

        /**
         * 内容类型: EXAM-考试 ASSIGNMENT-作业 SURVEY-调研 CONTENT-普通内容
         */
        private String contentType;

        /**
         * 内容ID
         */
        private Long contentId;

        /**
         * 内容标题
         */
        private String title;

        /**
         * 内容子类型(课程类型): video, document, series, article
         */
        private String type;

        /**
         * 封面图片
         */
        private String coverImage;

        /**
         * 是否必修: 0-选修 1-必修
         */
        private Integer isRequired;

        /**
         * 学习状态：completed-已完成，learning-学习中，not_started-未开始
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
    public static class LearnerInfo {
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
        private List<LearnerItem> completedList;

        /**
         * 未开始人员列表
         */
        private List<LearnerItem> notStartList;

        /**
         * 学习中人员列表
         */
        private List<LearnerItem> learningList;
    }

    /**
     * 学习人员项
     */
    @Data
    public static class LearnerItem {
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
         * 学习状态：completed-已完成，learning-学习中
         */
        private String status;
    }
}

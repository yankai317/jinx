package com.learn.dto.common;

/**
 * @author yujintao
 * @date 2025/5/14
 */

import lombok.Data;

/**
 * 学习人员信息
 */
@Data
public class LearnerDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 工号
     */
    private String employeeNo;

    /**
     * 部门
     */
    private String department;

    /**
     * 学习来源
     */
    private String source;

    /**
     * 完成状态
     * @see com.learn.constants.LearningStatus
     */
    private String status;

    /**
     * 学习时长(分钟)
     */
    private Integer studyDuration;

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
    private String deadline;

    /**
     * 完成时间
     */
    private String completionTime;

    /**
     * 最后学习时间
     */
    private String lastStudyTime;

    /**
     * 学习进度（百分比）
     */
    private Integer progress;
}

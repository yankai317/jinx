package com.learn.dto.toc.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 学习任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTaskDTO {
    /**
     * 任务ID
     */
    private Long id;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务类型：train-培训，map-学习地图
     */
    private String type;

    /**
     * 封面图片
     */
    private String coverImage;

    /**
     * 学分
     */
    private Integer credit;

    /**
     * 是否必修：1-必修，0-选修
     */
    private Integer isRequired;

    /**
     * 学习进度（百分比）
     */
    private Integer progress;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 是否置顶：1-是，0-否
     */
    private Integer isTop;
    
    /**
     * 任务组成数量（课程数、考试数等）
     */
    private Integer taskCount;
}

package com.learn.dto.toc.learning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 记录学习进度响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordLearningProgressResponse {
    /**
     * 学习进度（百分比）
     */
    private Integer progress;

    /**
     * 学习时长（分钟）
     */
    private Integer studyDuration;

    /**
     * 学习状态：learning-学习中，completed-已完成
     */
    private String status;

    /**
     * 最后学习时间
     */
    private String lastStudyTime;
}

package com.learn.dto.toc.personal;

import lombok.Data;

/**
 * 获取用户学习记录请求
 */
@Data
public class UserLearningRecordsRequest {
    /**
     * 记录类型：all-全部，course-课程，train-培训，map-地图，默认全部
     */
    private String type;

    /**
     * 来源。
     */
    private String source;
    
    /**
     * 完成状态：0-全部，1-学习中，2-已完成，默认0
     */
    private Integer status;
    
    /**
     * 开始时间，格式：yyyy-MM-dd
     */
    private String startTime;
    
    /**
     * 结束时间，格式：yyyy-MM-dd
     */
    private String endTime;
    
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;
}

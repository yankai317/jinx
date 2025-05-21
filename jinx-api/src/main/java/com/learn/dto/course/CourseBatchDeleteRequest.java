package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 批量删除课程请求
 */
@Data
public class CourseBatchDeleteRequest {
    /**
     * 课程ID列表
     */
    private List<Long> ids;
}

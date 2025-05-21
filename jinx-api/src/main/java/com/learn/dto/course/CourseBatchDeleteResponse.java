package com.learn.dto.course;

import lombok.Data;

import java.util.List;

/**
 * 批量删除课程响应
 */
@Data
public class CourseBatchDeleteResponse {
    /**
     * 成功删除数量
     */
    private Integer success;
    
    /**
     * 删除失败数量
     */
    private Integer failed;
    
    /**
     * 删除失败的ID列表
     */
    private List<Long> failedIds;
}

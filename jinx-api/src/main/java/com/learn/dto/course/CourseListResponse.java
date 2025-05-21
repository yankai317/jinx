package com.learn.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 课程列表响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseListResponse {
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 课程列表
     */
    private List<CourseListDTO> list;
}

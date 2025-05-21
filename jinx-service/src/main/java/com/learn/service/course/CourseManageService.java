package com.learn.service.course;

import com.learn.dto.course.CourseBatchDeleteResponse;
import com.learn.dto.course.CourseCreateRequest;
import com.learn.dto.course.CourseUpdateRequest;
import com.learn.infrastructure.repository.entity.Courses;

import java.util.List;

/**
 * @author yujintao
 * @description 定义课程管理接口，例如课程的创建/编辑/删除
 * @date 2025/4/21
 */
public interface CourseManageService {
    
    /**
     * 发布课程
     *
     * @param id 课程ID
     * @param userId 操作用户ID
     * @param userName 操作用户名称
     * @return 发布后的课程信息
     */
    Courses publishCourse(Long id, Long userId, String userName);
    
    /**
     * 取消发布课程
     *
     * @param id 课程ID
     * @param userId 操作用户ID
     * @param userName 操作用户名称
     * @return 取消发布后的课程信息
     */
    Courses unpublishCourse(Long id, Long userId, String userName);
    
    /**
     * 创建课程
     *
     * @param request 创建课程请求
     * @return 创建的课程信息
     */
    Courses createCourse(CourseCreateRequest request);
    
    /**
     * 更新课程
     *
     * @param request 更新课程请求
     * @return 更新后的课程信息
     */
    Courses updateCourse(CourseUpdateRequest request);
    
    /**
     * 删除课程
     *
     * @param id 课程ID
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 是否删除成功
     */
    boolean deleteCourse(Long id, Long updaterId, String updaterName);
    
    /**
     * 批量删除课程
     *
     * @param ids 课程ID列表
     * @param updaterId 更新人ID
     * @param updaterName 更新人名称
     * @return 批量删除结果
     */
    CourseBatchDeleteResponse batchDeleteCourse(List<Long> ids, Long updaterId, String updaterName);

    /**
     * 批量更新课程创建人
     *
     * @param courseIds 课程ID列表
     * @param newCreatorId 新的创建人ID
     * @param newCreatorName 新的创建人名称
     * @param operatorId 操作人ID
     * @param operatorName 操作人名称
     * @return 更新结果
     */
    boolean batchUpdateCreator(List<Long> courseIds, Long newCreatorId, String newCreatorName, Long operatorId, String operatorName);
}

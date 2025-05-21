package com.learn.service.user;

import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.user.UserStudyRecordCreateDTO;
import com.learn.dto.user.UserStudyRecordDTO;
import com.learn.infrastructure.repository.entity.UserLearningTask;

import java.util.List;

/**
 * @author yujintao
 * @date 2025/5/6
 */
public interface UserStudyService {

    /**
     * 创建用户学习记录，当用户自学或者被指派课程时，需要创建用户学习记录
     * 1. 业务类型为课程（非系列课），对应表插入记录： user_learning_task 。source仅为自学
     * 2. 业务类型为课程且为系列课，对应表插入记录： user_learning_task。source仅为自学
     * 3. 业务类型为培训，对应表插入记录： user_learning_task。培训中的课程也生成记录，parentId为培训id。
     * 4. 业务类型为地图，对应表插入记录： user_learning_task。地图的阶段也生成记录，parentId为地图id。地图阶段内的课程也生成记录，parentId为地图阶段id
     * 5. 如果是培训和地图，支持指派。查询指派记录表assignment_detail来确定是否为指派。
     * 6. 地图类型的扩展属性中需要记录 已完成阶段数，已完成必修任务数，已完成选修任务数，已获得学分；培训类型的扩展属性中需要记录 已完成课程数，已完成必修课程数，已完成选修课程数，已获得学分
     * @param userStudyRecordCreateDTO
     */
    void createUserStudyRecord(UserStudyRecordCreateDTO userStudyRecordCreateDTO);

    /**
     * 获取学习任务记录
     * @param userId
     * @param request
     * @return
     */

    UserLearningTask getLearningTask(Long userId, RecordLearningProgressRequest request);

    /**
     * 获取或创建学习任务记录
     * @param userId
     * @param request
     * @return
     */
    UserLearningTask getOrCreateLearningTask(Long userId, RecordLearningProgressRequest request);


    /**
     * 更新学习进度
     *
     * @param learningTask 学习任务
     * @param request 学习进度请求
     */
    void updateLearningProgress(UserLearningTask learningTask, RecordLearningProgressRequest request);


    /**
     * 处理任务完成
     * @param userId
     * @param bizType
     * @param bizId
     */
    void processCompletionTasks(Long userId, String bizType, Long bizId);
}

package com.learn.service.learning.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.enums.AssignTypeEnums;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.infrastructure.repository.entity.AssignRecords;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.AssignRecordsMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.UserCertificateMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.learning.LearningStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.learn.constants.BizType.LEARNING_MAP;

/**
 * 学习统计服务实现类
 */
@Service
@Slf4j
public class LearningStatisticsServiceImpl implements LearningStatisticsService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private UserCertificateMapper userCertificateMapper;
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private AssignRecordsMapper assignRecordsMapper;

    /**
     * 获取用户课程学习数量
     *
     * @param userId 用户ID
     * @return 课程学习数量
     */
    @Override
    public Long getUserCourseCount(Long userId) {
        try {
            LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, BizType.COURSE)
                    .eq(UserLearningTask::getIsDel, 0);
            
            return userLearningTaskMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("获取用户课程学习数量异常, userId: {}", userId, e);
            return 0L;
        }
    }

    /**
     * 获取用户培训完成率
     *
     * @param userId 用户ID
     * @return 培训完成率（百分比）
     */
    @Override
    public double getUserTrainCompletionRate(Long userId) {
        try {
            // 查询用户参与的所有培训
            LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, BizType.TRAIN)
                    .eq(UserLearningTask::getIsDel, 0);
            
            List<UserLearningTask> trainTasks = userLearningTaskMapper.selectList(wrapper);
            
            if (trainTasks.isEmpty()) {
                return 0.0;
            }
            
            // 计算完成率
            int totalCount = trainTasks.size();
            int completedCount = 0;
            
            for (UserLearningTask task : trainTasks) {
                if (task.getStatus() != null && LearningStatus.COMPLETED.equals(task.getStatus())) { // 状态为已完成
                    completedCount++;
                }
            }
            
            return totalCount > 0 ? (double) completedCount / totalCount * 100 : 0.0;
        } catch (Exception e) {
            log.error("获取用户培训完成率异常, userId: {}", userId, e);
            return 0.0;
        }
    }

    /**
     * 获取用户学习地图数量
     *
     * @param userId 用户ID
     * @return 学习地图数量
     */
    @Override
    public Long getUserMapCount(Long userId) {
        try {
            LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, LEARNING_MAP)
                    .eq(UserLearningTask::getIsDel, 0);
            
            return userLearningTaskMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("获取用户学习地图数量异常, userId: {}", userId, e);
            return 0L;
        }
    }

    /**
     * 获取用户证书数量
     *
     * @param userId 用户ID
     * @return 证书数量
     */
    @Override
    public Long getUserCertificateCount(Long userId) {
        try {
            LambdaQueryWrapper<UserCertificate> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserCertificate::getUserId, userId)
                    .eq(UserCertificate::getStatus, 0) // 有效状态
                    .eq(UserCertificate::getIsDel, 0);
            
            return userCertificateMapper.selectCount(wrapper);
        } catch (Exception e) {
            log.error("获取用户证书数量异常, userId: {}", userId, e);
            return 0L;
        }
    }

    /**
     * 获取用户最近学习的课程
     *
     * @param userId 用户ID
     * @return 最近学习的课程列表
     */
    @Override
    public List<Map<String, Object>> getUserRecentCourses(Long userId) {
        try {
            // 查询用户最近学习的课程
            LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, BizType.COURSE)
                    .eq(UserLearningTask::getIsDel, 0)
                    .orderByDesc(UserLearningTask::getLastStudyTime)
                    .last("LIMIT 5"); // 最近5条记录
            
            List<UserLearningTask> courseTasks = userLearningTaskMapper.selectList(wrapper);
            
            List<Map<String, Object>> result = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (UserLearningTask task : courseTasks) {
                Map<String, Object> course = new HashMap<>();
                course.put("id", task.getBizId());
                // 这里应该关联课程表获取课程名称，暂时使用模拟数据
                course.put("name", "课程" + task.getBizId());
                course.put("studyDuration", task.getStudyDuration());
                course.put("lastStudyTime", task.getLastStudyTime() != null ? sdf.format(task.getLastStudyTime()) : "");
                result.add(course);
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取用户最近学习的课程异常, userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户最近学习的培训
     *
     * @param userId 用户ID
     * @return 最近学习的培训列表
     */
    @Override
    public List<Map<String, Object>> getUserRecentTrains(Long userId) {
        try {
            // 查询用户最近学习的培训
            LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, BizType.TRAIN)
                    .eq(UserLearningTask::getIsDel, 0)
                    .orderByDesc(UserLearningTask::getLastStudyTime)
                    .last("LIMIT 5"); // 最近5条记录
            
            List<UserLearningTask> trainTasks = userLearningTaskMapper.selectList(wrapper);
            
            List<Map<String, Object>> result = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (UserLearningTask task : trainTasks) {
                Map<String, Object> train = new HashMap<>();
                train.put("id", task.getBizId());
                // 这里应该关联培训表获取培训名称，暂时使用模拟数据
                train.put("name", "培训" + task.getBizId());
                
                // 计算进度
                train.put("progress", task.getProgress() != null ? task.getProgress() : 0);
                train.put("lastStudyTime", task.getLastStudyTime() != null ? sdf.format(task.getLastStudyTime()) : "");
                result.add(train);
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取用户最近学习的培训异常, userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取用户最近学习的地图
     *
     * @param userId 用户ID
     * @return 最近学习的地图列表
     */
    @Override
    public List<Map<String, Object>> getUserRecentMaps(Long userId) {
        try {
            // 查询用户最近学习的地图
            LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getBizType, LEARNING_MAP)
                    .eq(UserLearningTask::getIsDel, 0)
                    .orderByDesc(UserLearningTask::getLastStudyTime)
                    .last("LIMIT 5"); // 最近5条记录
            
            List<UserLearningTask> mapTasks = userLearningTaskMapper.selectList(wrapper);
            
            List<Map<String, Object>> result = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            for (UserLearningTask task : mapTasks) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", task.getBizId());
                // 这里应该关联地图表获取地图名称，暂时使用模拟数据
                map.put("name", "学习地图" + task.getBizId());
                
                // 查询当前阶段
                LambdaQueryWrapper<UserLearningTask> stageWrapper = new LambdaQueryWrapper<>();
                stageWrapper.eq(UserLearningTask::getUserId, userId)
                        .eq(UserLearningTask::getParentId, task.getId())
                        .eq(UserLearningTask::getBizType, BizType.MAP_STAGE)
                        .eq(UserLearningTask::getIsDel, 0)
                        .orderByDesc(UserLearningTask::getLastStudyTime)
                        .last("LIMIT 1");
                
                UserLearningTask currentStage = userLearningTaskMapper.selectOne(stageWrapper);
                if (currentStage != null) {
                    map.put("currentStage", "阶段" + currentStage.getBizId());
                } else {
                    map.put("currentStage", "未开始");
                }
                
                // 计算进度
                map.put("progress", task.getProgress() != null ? task.getProgress() : 0);
                map.put("lastStudyTime", task.getLastStudyTime() != null ? sdf.format(task.getLastStudyTime()) : "");
                
                result.add(map);
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取用户最近学习的地图异常, userId: {}", userId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 设置学习地图自动指派状态
     *
     * @param mapId 学习地图ID
     * @param enableAutoAssign 是否启用自动指派
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setLearningMapAutoAssign(Long mapId, Boolean enableAutoAssign) {
        try {
            // 1. 查询学习地图
            LearningMap learningMap = learningMapMapper.selectById(mapId);
            if (learningMap == null || learningMap.getIsDel() == 1) {
                log.error("学习地图不存在或已删除, mapId: {}", mapId);
                return false;
            }
            
            // 2. 更新学习地图的自动指派属性
            LearningMap updateLearningMap = new LearningMap()
                    .setId(learningMap.getId())
                    .setAttributes(learningMap.getAttributes());
            updateLearningMap.appendAttributes(LearningMap.ATTRIBUTES_KEY_ENABLE_AUTO_ASSIGN, enableAutoAssign);
            learningMapMapper.updateById(updateLearningMap);
            
            // 3. 处理指派记录
            
            // 查询最近的一条自动指派记录
            LambdaQueryWrapper<AssignRecords> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AssignRecords::getType, LEARNING_MAP)
                    .eq(AssignRecords::getTypeId, mapId)
                    .eq(AssignRecords::getAssignType, AssignTypeEnums.AUTO.getCode())
                    .eq(AssignRecords::getIsDel, 0)
                    .orderByDesc(AssignRecords::getGmtCreate)
                    .last("LIMIT 1");
            
            AssignRecords assignRecord = assignRecordsMapper.selectOne(wrapper);
            
            // 如果找到了指派记录
            if (assignRecord != null) {
                // 根据enableAutoAssign设置状态
//                if (enableAutoAssign) {
//                    // 开启自动指派
//                    assignRecord.setStatus(AssignStatusEnums.WAIT.getCode());
//                } else {
                // 关闭自动指派
                assignRecord.setIsDel(Boolean.TRUE);
//                }
                
                assignRecordsMapper.updateById(assignRecord);
            }
            // 如果没有找到指派记录，则跳过处理
            
            return true;
        } catch (Exception e) {
            log.error("设置学习地图自动指派状态异常, mapId: {}, enableAutoAssign: {}", mapId, enableAutoAssign, e);
            throw e;
        }
    }
}

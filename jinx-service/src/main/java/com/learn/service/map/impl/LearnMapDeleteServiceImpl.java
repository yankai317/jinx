package com.learn.service.map.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.map.LearningMapBatchDeleteResponse;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.LearningMapStage;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.LearningMapStageMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeDeleteRequest;
import com.learn.service.map.LearnMapDeleteService;
import com.learn.service.operation.OperationLogService;
import com.learn.common.dto.UserTokenInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习地图删除服务实现类
 */
@Service
@Slf4j
public class LearnMapDeleteServiceImpl implements LearnMapDeleteService {
    
    @Autowired
    private LearningMapMapper learningMapMapper;
    
    @Autowired
    private LearningMapStageMapper learningMapStageMapper;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private CommonRangeInterface commonRangeInterface;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 删除学习地图
     * 
     * @param id 学习地图ID
     * @param userId 操作用户ID
     * @param userName 操作用户名称
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteLearningMap(Long id, Long userId, String userName) {
        log.info("删除学习地图，id: {}, 操作用户: {}", id, userName);
        // 1. 检查学习地图是否存在
        LearningMap learningMap = learningMapMapper.selectById(id);
        if (learningMap == null || learningMap.getIsDel() == 1) {
            throw new CommonException("学习地图不存在");
        }
        
        // 2. 检查是否有学员正在学习该地图
        LambdaQueryWrapper<UserLearningTask> progressQueryWrapper = new LambdaQueryWrapper<>();
        progressQueryWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                .eq(UserLearningTask::getBizId, id)
                .eq(UserLearningTask::getIsDel, 0);
        Long studyingCount = userLearningTaskMapper.selectCount(progressQueryWrapper);
        if (studyingCount > 0) {
            throw new CommonException("该学习地图有学员正在学习，不能删除");
        }
        // 3. 逻辑删除学习地图
        LambdaUpdateWrapper<LearningMap> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LearningMap::getId, id)
                .set(LearningMap::getIsDel, 1)
                .set(LearningMap::getUpdaterId, userId)
                .set(LearningMap::getUpdaterName, userName)
                .set(LearningMap::getGmtModified, new Date());
        learningMapMapper.update(null, updateWrapper);
        
        // 4.逻辑删除学习地图阶段
        LambdaUpdateWrapper<LearningMapStage> stageUpdateWrapper = new LambdaUpdateWrapper<>();
        stageUpdateWrapper.eq(LearningMapStage::getMapId, id)
                .set(LearningMapStage::getIsDel, 1)
                .set(LearningMapStage::getUpdaterId, userId)
                .set(LearningMapStage::getUpdaterName, userName)
                .set(LearningMapStage::getGmtModified, new Date());
        learningMapStageMapper.update(null, stageUpdateWrapper);
        
        // 5. 逻辑删除学习地图相关的用户学习任务记录
        LambdaUpdateWrapper<UserLearningTask> taskUpdateWrapper = new LambdaUpdateWrapper<>();
        taskUpdateWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                .eq(UserLearningTask::getBizId, id)
                .set(UserLearningTask::getIsDel, 1)
                .set(UserLearningTask::getUpdaterId, userId)
                .set(UserLearningTask::getUpdaterName, userName)
                .set(UserLearningTask::getGmtModified, new Date());
        userLearningTaskMapper.update(null, taskUpdateWrapper);
        
        // 6. 查询学习地图的所有阶段
        LambdaQueryWrapper<LearningMapStage> stageQueryWrapper = new LambdaQueryWrapper<>();
        stageQueryWrapper.eq(LearningMapStage::getMapId, id);
        List<LearningMapStage> stages = learningMapStageMapper.selectList(stageQueryWrapper);
        
        // 7. 逻辑删除阶段关联的内容
        for (LearningMapStage stage : stages) {
            LambdaUpdateWrapper<ContentRelation> contentRelationUpdateWrapper = new LambdaUpdateWrapper<>();
            contentRelationUpdateWrapper.eq(ContentRelation::getBizId, stage.getId())
                    .eq(ContentRelation::getBizType, "MAP_STAGE")
                    .set(ContentRelation::getIsDel, 1)
                    .set(ContentRelation::getUpdaterId, userId)
                    .set(ContentRelation::getUpdaterName, userName)
                    .set(ContentRelation::getGmtModified, new Date());
            contentRelationMapper.update(null, contentRelationUpdateWrapper);
            
            // 8. 逻辑删除阶段相关的用户学习任务记录
            LambdaUpdateWrapper<UserLearningTask> stageTaskUpdateWrapper = new LambdaUpdateWrapper<>();
            stageTaskUpdateWrapper.eq(UserLearningTask::getBizType, BizType.MAP_STAGE)
                    .eq(UserLearningTask::getBizId, stage.getId())
                    .set(UserLearningTask::getIsDel, 1)
                    .set(UserLearningTask::getUpdaterId, userId)
                    .set(UserLearningTask::getUpdaterName, userName)
                    .set(UserLearningTask::getGmtModified, new Date());
            userLearningTaskMapper.update(null, stageTaskUpdateWrapper);
        }
        // 9. 删除范围模块中的相关配置
        deleteRangeConfig(id);
        
        // 记录操作日志
        try {
            UserTokenInfo userInfo = new UserTokenInfo();
            userInfo.setUserId(userId);
            // 构建操作详情JSON
            Map<String, Object> operationDetail = new HashMap<>();
            operationDetail.put("id", id);
            operationDetail.put("name", learningMap.getName());
            
            String operationDetailJson = objectMapper.writeValueAsString(operationDetail);
            operationLogService.recordDeleteOperation(id, BizType.LEARNING_MAP, operationDetailJson, userInfo);
        } catch (Exception e) {
            log.error("记录删除学习地图操作日志失败", e);
            // 不影响主流程，继续执行
        }
        
        log.info("学习地图删除成功，id: {}", id);
        return true;
    }
    
    /**
     * 批量删除学习地图
     * 
     * @param ids 学习地图ID列表
     * @param userId 操作用户ID
     * @param userName 操作用户名称
     * @return 批量删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningMapBatchDeleteResponse batchDeleteLearningMap(List<Long> ids, Long userId, String userName) {
        log.info("批量删除学习地图，ids: {}, 操作用户: {}", ids, userName);
        
        LearningMapBatchDeleteResponse response = new LearningMapBatchDeleteResponse();
        int successCount = 0;
        int failCount = 0;
        
        if (ids == null || ids.isEmpty()) {
            response.setSuccessCount(successCount);
            response.setFailCount(failCount);
            return response;
        }
        
        try {
            // 1. 检查学习地图是否存在
            List<LearningMap> learningMaps = learningMapMapper.selectBatchIds(ids);
            if (learningMaps.size() != ids.size()) {
                log.warn("部分学习地图不存在");
                failCount = ids.size() - learningMaps.size();
                // 过滤出存在的学习地图ID
                ids = learningMaps.stream()
                        .filter(map -> map.getIsDel() == 0)
                        .map(LearningMap::getId)
                        .toList();
            }
            
            if (ids.isEmpty()) {
                response.setSuccessCount(successCount);
                response.setFailCount(failCount);
                return response;
            }
            
            // 2. 检查是否有学员正在学习这些地图
            LambdaQueryWrapper<UserLearningTask> progressQueryWrapper = new LambdaQueryWrapper<>();
            progressQueryWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                    .in(UserLearningTask::getBizId, ids)
                    .eq(UserLearningTask::getIsDel, 0);
            List<UserLearningTask> progressList = userLearningTaskMapper.selectList(progressQueryWrapper);
            
            if (!progressList.isEmpty()) {
                // 获取有学员正在学习的地图ID
                List<Long> studyingMapIds = progressList.stream()
                        .map(UserLearningTask::getBizId)
                        .distinct()
                        .toList();
                
                log.warn("以下学习地图有学员正在学习，不能删除: {}", studyingMapIds);
                failCount += studyingMapIds.size();
                
                // 过滤出可以删除的地图ID
                ids = ids.stream()
                        .filter(id -> !studyingMapIds.contains(id))
                        .toList();
            }
            
            if (ids.isEmpty()) {
                response.setSuccessCount(successCount);
                response.setFailCount(failCount);
                return response;
            }
            
            Date now = new Date();
            
            // 3. 批量逻辑删除学习地图
            LambdaUpdateWrapper<LearningMap> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(LearningMap::getId, ids)
                    .set(LearningMap::getIsDel, 1)
                    .set(LearningMap::getUpdaterId, userId)
                    .set(LearningMap::getUpdaterName, userName)
                    .set(LearningMap::getGmtModified, now);
            learningMapMapper.update(null, updateWrapper);
            
            // 4. 批量逻辑删除学习地图阶段
            LambdaUpdateWrapper<LearningMapStage> stageUpdateWrapper = new LambdaUpdateWrapper<>();
            stageUpdateWrapper.in(LearningMapStage::getMapId, ids)
                    .set(LearningMapStage::getIsDel, 1)
                    .set(LearningMapStage::getUpdaterId, userId)
                    .set(LearningMapStage::getUpdaterName, userName)
                    .set(LearningMapStage::getGmtModified, now);
            learningMapStageMapper.update(null, stageUpdateWrapper);
            
            // 5. 批量逻辑删除学习地图相关的用户学习任务记录
            LambdaUpdateWrapper<UserLearningTask> taskUpdateWrapper = new LambdaUpdateWrapper<>();
            taskUpdateWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                    .in(UserLearningTask::getBizId, ids)
                    .set(UserLearningTask::getIsDel, 1)
                    .set(UserLearningTask::getUpdaterId, userId)
                    .set(UserLearningTask::getUpdaterName, userName)
                    .set(UserLearningTask::getGmtModified, now);
            userLearningTaskMapper.update(null, taskUpdateWrapper);
            
            // 6. 查询这些学习地图的所有阶段
            LambdaQueryWrapper<LearningMapStage> stageQueryWrapper = new LambdaQueryWrapper<>();
            stageQueryWrapper.in(LearningMapStage::getMapId, ids);
            List<LearningMapStage> stages = learningMapStageMapper.selectList(stageQueryWrapper);
            
            if (!stages.isEmpty()) {
                // 获取所有阶段ID
                List<Long> stageIds = stages.stream()
                        .map(LearningMapStage::getId)
                        .toList();
                
                // 7. 批量逻辑删除阶段关联的内容
                LambdaUpdateWrapper<ContentRelation> contentRelationUpdateWrapper = new LambdaUpdateWrapper<>();
                contentRelationUpdateWrapper.in(ContentRelation::getBizId, stageIds)
                        .eq(ContentRelation::getBizType, "MAP_STAGE")
                        .set(ContentRelation::getIsDel, 1)
                        .set(ContentRelation::getUpdaterId, userId)
                        .set(ContentRelation::getUpdaterName, userName)
                        .set(ContentRelation::getGmtModified, now);
                contentRelationMapper.update(null, contentRelationUpdateWrapper);
                
                // 8. 批量逻辑删除阶段相关的用户学习任务记录
                LambdaUpdateWrapper<UserLearningTask> stageTaskUpdateWrapper = new LambdaUpdateWrapper<>();
                stageTaskUpdateWrapper.eq(UserLearningTask::getBizType, BizType.MAP_STAGE)
                        .in(UserLearningTask::getBizId, stageIds)
                        .set(UserLearningTask::getIsDel, 1)
                        .set(UserLearningTask::getUpdaterId, userId)
                        .set(UserLearningTask::getUpdaterName, userName)
                        .set(UserLearningTask::getGmtModified, now);
                userLearningTaskMapper.update(null, stageTaskUpdateWrapper);
            }
            
            // 9. 批量删除范围模块中的相关配置
            for (Long mapId : ids) {
                deleteRangeConfig(mapId);
            }
            
            successCount = ids.size();
            
        } catch (Exception e) {
            log.error("批量删除学习地图失败", e);
            failCount = ids.size();
            throw e;
        }
        
        response.setSuccessCount(successCount);
        response.setFailCount(failCount);
        
        // 记录批量删除操作日志
        if (successCount > 0) {
            try {
                UserTokenInfo userInfo = new UserTokenInfo();
                userInfo.setUserId(userId);
                
                // 构建操作详情JSON
                Map<String, Object> operationDetail = new HashMap<>();
                operationDetail.put("ids", ids);
                operationDetail.put("successCount", successCount);
                operationDetail.put("failCount", failCount);
                
                String operationDetailJson = objectMapper.writeValueAsString(operationDetail);
                // 使用第一个ID作为业务ID记录日志
                operationLogService.recordDeleteOperation(ids.get(0), BizType.LEARNING_MAP, operationDetailJson, userInfo);
            } catch (Exception e) {
                log.error("记录批量删除学习地图操作日志失败", e);
                // 不影响主流程，继续执行
            }
        }
        
        log.info("批量删除学习地图完成，成功: {}, 失败: {}", successCount, failCount);
        return response;
    }
    
    /**
     * 删除范围模块中的相关配置
     * 
     * @param mapId 学习地图ID
     */
    private void deleteRangeConfig(Long mapId) {
        // 删除可见范围配置
        CommonRangeDeleteRequest visibleRangeRequest = new CommonRangeDeleteRequest();
        visibleRangeRequest.setModelType("VISIBLE_RANGE");
        visibleRangeRequest.setType(BizType.LEARNING_MAP);
        visibleRangeRequest.setTypeId(mapId);
        commonRangeInterface.deleteRangeByBusinessId(visibleRangeRequest);
        
        // 删除协同管理范围配置
        CommonRangeDeleteRequest collaborationRangeRequest = new CommonRangeDeleteRequest();
        collaborationRangeRequest.setModelType("COLLABORATION_RANGE");
        collaborationRangeRequest.setType(BizType.LEARNING_MAP);
        collaborationRangeRequest.setTypeId(mapId);
        commonRangeInterface.deleteRangeByBusinessId(collaborationRangeRequest);
        
        // 删除任务指派范围配置
        CommonRangeDeleteRequest assignRangeRequest = new CommonRangeDeleteRequest();
        assignRangeRequest.setModelType("ASSIGN_RANGE");
        assignRangeRequest.setType(BizType.LEARNING_MAP);
        assignRangeRequest.setTypeId(mapId);
        commonRangeInterface.deleteRangeByBusinessId(assignRangeRequest);
    }
}

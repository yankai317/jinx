package com.learn.service.train.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learn.common.dto.UserTokenInfo;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.train.TrainBatchDeleteResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeDeleteRequest;
import com.learn.service.train.TrainDeleteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 培训删除服务实现类
 */
@Service
@Slf4j
public class TrainDeleteServiceImpl implements TrainDeleteService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    /**
     * 删除培训
     *
     * @param id 培训ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTrain(Long id, UserTokenInfo userInfo) {
        // 参数校验
        if (id == null || id <= 0) {
            throw new CommonException("培训ID不能为空或小于等于0");
        }

        // 获取培训信息
        Train train = trainMapper.selectById(id);
        if (train == null || train.getIsDel() == 1) {
            throw new CommonException("培训不存在或已删除");
        }

        // 检查培训是否有学员正在学习
        checkTrainLearningStatus(id);
        
        // 检查培训是否有有效引用
        checkTrainReference(id);

        // 获取当前用户信息
        Long userId = userInfo.getUserId();
        String userName = userInfo.getName();

        // 逻辑删除培训
        deleteTrainData(id, userId, userName);

        return true;
    }
    
    /**
     * 批量删除培训
     *
     * @param ids 培训ID列表
     * @return 批量删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainBatchDeleteResponse batchDeleteTrain(List<Long> ids, UserTokenInfo userInfo) {

        // 获取当前用户信息
        Long userId = userInfo.getUserId();
        String userName = userInfo.getName();
        TrainBatchDeleteResponse response = new TrainBatchDeleteResponse();
        
        if (CollectionUtils.isEmpty(ids)) {
            throw new CommonException("培训ID列表不能为空");
        }
        
        // 获取培训信息
        LambdaQueryWrapper<Train> trainQueryWrapper = new LambdaQueryWrapper<>();
        trainQueryWrapper.in(Train::getId, ids)
                .eq(Train::getIsDel, 0);
        List<Train> trains = trainMapper.selectList(trainQueryWrapper);
        
        // 检查是否所有培训都存在
        if (trains.size() != ids.size()) {
            Set<Long> existingIds = trains.stream().map(Train::getId).collect(Collectors.toSet());
            List<Long> notExistIds = ids.stream().filter(id -> !existingIds.contains(id)).collect(Collectors.toList());
            throw new CommonException("以下培训不存在或已删除: " + notExistIds);
        }
        
        // 批量检查培训是否有学员正在学习
        List<Long> learningIds = batchCheckTrainLearningStatus(ids);
        if (!learningIds.isEmpty()) {
            throw new CommonException("以下培训有学员正在学习，不能删除: " + learningIds);
        }
        
        // 批量检查培训是否有有效引用
        List<Long> referencedIds = batchCheckTrainReference(ids);
        if (!referencedIds.isEmpty()) {
            throw new CommonException("以下培训被其他内容引用，不能删除: " + referencedIds);
        }

        
        // 批量逻辑删除培训
        batchDeleteTrainData(ids, userId, userName);
        
        response.setSuccessCount(ids.size());
        response.setFailCount(0);
        
        return response;
    }

    /**
     * 检查培训是否有学员正在学习
     *
     * @param trainId 培训ID
     */
    private void checkTrainLearningStatus(Long trainId) {
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.TRAIN)
                .eq(UserLearningTask::getBizId, trainId)
                .eq(UserLearningTask::getIsDel, 0)
                .eq(UserLearningTask::getStatus, 1); // 状态为学习中(1)

        long count = userLearningTaskMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new CommonException("培训有学员正在学习，不能删除");
        }
    }
    
    /**
     * 批量检查培训是否有学员正在学习
     *
     * @param trainIds 培训ID列表
     * @return 有学员正在学习的培训ID列表
     */
    private List<Long> batchCheckTrainLearningStatus(List<Long> trainIds) {
        if (CollectionUtils.isEmpty(trainIds)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.TRAIN)
                .in(UserLearningTask::getBizId, trainIds)
                .eq(UserLearningTask::getIsDel, 0)
                .eq(UserLearningTask::getStatus, 1); // 状态为学习中(1)
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        if (CollectionUtils.isEmpty(learningTasks)) {
            return Collections.emptyList();
        }
        
        return learningTasks.stream()
                .map(UserLearningTask::getBizId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * 检查培训是否有有效引用
     * 
     * @param trainId 培训ID
     */
    private void checkTrainReference(Long trainId) {
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getContentType, "TRAIN")
                .eq(ContentRelation::getContentId, trainId)
                .eq(ContentRelation::getIsDel, 0);
                
        long count = contentRelationMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new CommonException("培训被其他内容引用，不能删除");
        }
    }
    
    /**
     * 批量检查培训是否有有效引用
     *
     * @param trainIds 培训ID列表
     * @return 有有效引用的培训ID列表
     */
    private List<Long> batchCheckTrainReference(List<Long> trainIds) {
        if (CollectionUtils.isEmpty(trainIds)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getContentType, "TRAIN")
                .in(ContentRelation::getContentId, trainIds)
                .eq(ContentRelation::getIsDel, 0);
        
        List<ContentRelation> contentRelations = contentRelationMapper.selectList(queryWrapper);
        
        if (CollectionUtils.isEmpty(contentRelations)) {
            return Collections.emptyList();
        }
        
        return contentRelations.stream()
                .map(ContentRelation::getContentId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 逻辑删除培训及相关数据
     *
     * @param trainId  培训ID
     * @param userId   用户ID
     * @param userName 用户名称
     */
    private void deleteTrainData(Long trainId, Long userId, String userName) {
        Date now = new Date();

        // 1. 逻辑删除培训
        LambdaUpdateWrapper<Train> trainUpdateWrapper = new LambdaUpdateWrapper<>();
        trainUpdateWrapper.eq(Train::getId, trainId)
                .set(Train::getIsDel, 1)
                .set(Train::getUpdaterId, userId)
                .set(Train::getUpdaterName, userName)
                .set(Train::getGmtModified, now);
        trainMapper.update(null, trainUpdateWrapper);

        // 2. 逻辑删除培训内容关联
        LambdaUpdateWrapper<ContentRelation> contentUpdateWrapper = new LambdaUpdateWrapper<>();
        contentUpdateWrapper.eq(ContentRelation::getBizId, trainId)
                .eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getIsDel, 0)
                .set(ContentRelation::getIsDel, 1)
                .set(ContentRelation::getUpdaterId, userId)
                .set(ContentRelation::getUpdaterName, userName)
                .set(ContentRelation::getGmtModified, now);
        contentRelationMapper.update(null, contentUpdateWrapper);
    }
    
    /**
     * 批量逻辑删除培训及相关数据
     *
     * @param trainIds 培训ID列表
     * @param userId   用户ID
     * @param userName 用户名称
     */
    private void batchDeleteTrainData(List<Long> trainIds, Long userId, String userName) {
        if (CollectionUtils.isEmpty(trainIds)) {
            return;
        }
        
        Date now = new Date();

        // 1. 批量逻辑删除培训
        LambdaUpdateWrapper<Train> trainUpdateWrapper = new LambdaUpdateWrapper<>();
        trainUpdateWrapper.in(Train::getId, trainIds)
                .set(Train::getIsDel, 1)
                .set(Train::getUpdaterId, userId)
                .set(Train::getUpdaterName, userName)
                .set(Train::getGmtModified, now);
        trainMapper.update(null, trainUpdateWrapper);

        // 2. 批量逻辑删除培训内容关联
        LambdaUpdateWrapper<ContentRelation> contentUpdateWrapper = new LambdaUpdateWrapper<>();
        contentUpdateWrapper.in(ContentRelation::getBizId, trainIds)
                .eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getIsDel, 0)
                .set(ContentRelation::getIsDel, 1)
                .set(ContentRelation::getUpdaterId, userId)
                .set(ContentRelation::getUpdaterName, userName)
                .set(ContentRelation::getGmtModified, now);
        contentRelationMapper.update(null, contentUpdateWrapper);
    }

}

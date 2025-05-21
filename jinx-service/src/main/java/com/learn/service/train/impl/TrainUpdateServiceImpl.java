package com.learn.service.train.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.learn.common.exception.CommonException;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.train.TrainUpdateRequest;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.FileService;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.train.TrainUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 培训更新服务实现类
 */
@Service
@Slf4j
public class TrainUpdateServiceImpl implements TrainUpdateService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired
    private FileService fileService;

    @Autowired
    private RangeSetService rangeSetService;
    @Autowired
    private UserMapper userMapper;

    /**
     * 更新培训信息
     *
     * @param request 更新培训请求
     * @return 是否更新成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTrain(TrainUpdateRequest request, Long userId) {
        // 参数校验
        if (request == null) {
            throw new CommonException("请求参数不能为空");
        }
        if (request.getId() == null || request.getId() <= 0) {
            throw new CommonException("培训ID不能为空或小于等于0");
        }

        User user = userMapper.selectById(userId);
        String userName = "";
        if (Objects.nonNull(user)) {
            userName = user.getNickname();
        }
        // 获取原培训信息，用于记录变更日志
        Train oldTrain = trainMapper.selectById(request.getId());
        if (oldTrain == null || oldTrain.getIsDel() == 1) {
            throw new CommonException("培训不存在或已删除");
        }

        // 更新培训基本信息
        Train train = buildTrainEntity(request, oldTrain, userId, userName);
        trainMapper.updateById(train);

        // 更新培训内容关联
        updateTrainContents(request, userId, userName);
        
        // 更新证书关联
        updateCertificateRelation(request, userId, userName);

        // 更新培训可见范围
        request.setOperatorId(userId);
        request.setOperatorName(userName);
        rangeSetService.updateVisibilityRange(BizType.TRAIN, request.getId(), request);

        // 更新培训协同管理
        rangeSetService.updateCollaborators(BizType.TRAIN, request.getId(), request);
        return true;
    }

    /**
     * 构建培训实体
     *
     * @param request  更新培训请求
     * @param oldTrain 原培训实体
     * @param userId   用户ID
     * @param userName 用户名称
     * @return 培训实体
     */
    private Train buildTrainEntity(TrainUpdateRequest request, Train oldTrain, Long userId, String userName) {
        Train train = new Train();
        train.setId(request.getId());
        
        // 只更新请求中包含的字段
        if (StringUtils.hasText(request.getTitle())) {
            train.setName(request.getTitle());
        }

        // 处理封面图URL，将urlKey解析为url
        if (StringUtils.hasText(request.getCover())) {
            train.setCover(parseUrlKey(request.getCover()));
        }

        if (request.getIntroduction() != null) {
            train.setIntroduction(request.getIntroduction());
        }

        if (request.getCredit() != null) {
            train.setCredit(request.getCredit());
        }

        // 处理分类IDs
        if (org.apache.commons.lang3.StringUtils.isNotBlank(request.getCategoryIds())) {
            train.setCategoryIds(request.getCategoryIds());
        }

        // 处理是否允许评论
        if (request.getAllowComment() != null) {
            train.setAllowComment(request.getAllowComment() ? 1 : 0);
        }
        
        // 处理是否可引用
        if (request.getIfIsCitable() != null) {
            train.setIfIsCitable(request.getIfIsCitable());
        }

        if (request.getCertificateId() != null) {
            train.setCertificateId(request.getCertificateId());
        }

        if (StringUtils.hasText(request.getStatus())) {
            train.setStatus(request.getStatus());
        }

        train.setUpdaterId(userId);
        train.setUpdaterName(userName);
        train.setGmtModified(new Date());

        return train;
    }

    /**
     * 更新培训内容关联
     *
     * @param request  更新培训请求
     * @param userId   用户ID
     * @param userName 用户名称
     */
    private void updateTrainContents(TrainUpdateRequest request, Long userId, String userName) {
        Long trainId = request.getId();

        // 删除指定的内容关联
        if (!CollectionUtils.isEmpty(request.getDeleteContentIds())) {
            for (Long contentId : request.getDeleteContentIds()) {
                LambdaUpdateWrapper<ContentRelation> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(ContentRelation::getId, contentId)
                        .eq(ContentRelation::getBizId, trainId)
                        .eq(ContentRelation::getBizType, BizType.TRAIN)
                        .eq(ContentRelation::getIsDel, 0)
                        .set(ContentRelation::getIsDel, 1)
                        .set(ContentRelation::getUpdaterId, userId)
                        .set(ContentRelation::getUpdaterName, userName)
                        .set(ContentRelation::getGmtModified, new Date());
                contentRelationMapper.update(null, updateWrapper);
            }
        }

        // 更新或新增内容关联
        if (!CollectionUtils.isEmpty(request.getContents())) {
            for (TrainUpdateRequest.TrainContentRequest content : request.getContents()) {
                if (content.getId() != null) {
                    // 更新现有内容关联
                    ContentRelation relation = new ContentRelation();
                    relation.setId(content.getId());
                    relation.setContentType(content.getType());
                    relation.setContentId(content.getContentId());
                    relation.setSortOrder(content.getSortOrder());
                    relation.setIsRequired(content.getIsRequired() != null ? (content.getIsRequired() ? 1 : 0) : null);
                    relation.setUpdaterId(userId);
                    relation.setUpdaterName(userName);
                    relation.setGmtModified(new Date());
                    
                    LambdaUpdateWrapper<ContentRelation> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.eq(ContentRelation::getId, content.getId())
                            .eq(ContentRelation::getBizId, trainId)
                            .eq(ContentRelation::getBizType, BizType.TRAIN)
                            .eq(ContentRelation::getIsDel, 0);
                    contentRelationMapper.update(relation, updateWrapper);
                } else {
                    // 新增内容关联
                    ContentRelation relation = new ContentRelation();
                    relation.setBizId(trainId);
                    relation.setBizType(BizType.TRAIN);
                    relation.setContentType(content.getType());
                    relation.setContentId(content.getContentId());
                    relation.setSortOrder(content.getSortOrder() != null ? content.getSortOrder() : 0);
                    relation.setIsRequired(content.getIsRequired() != null ? (content.getIsRequired() ? 1 : 0) : 1);
                    relation.setCreatorId(userId);
                    relation.setCreatorName(userName);
                    relation.setUpdaterId(userId);
                    relation.setUpdaterName(userName);
                    relation.setGmtCreate(new Date());
                    relation.setGmtModified(new Date());
                    relation.setIsDel(0);
                    contentRelationMapper.insert(relation);
                }
            }
        }
    }

    /**
     * 更新证书关联
     *
     * @param request  更新培训请求
     * @param userId   用户ID
     * @param userName 用户名称
     */
    private void updateCertificateRelation(TrainUpdateRequest request, Long userId, String userName) {
        if (request.getCertificateId() == null) {
            return;
        }

        Long trainId = request.getId();
        Date now = new Date();

        // 查询是否已存在证书关联
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizId, trainId)
                .eq(ContentRelation::getBizType, BizType.TRAIN)
                .eq(ContentRelation::getContentType, "CERTIFICATE")
                .eq(ContentRelation::getContentBizType, "CERTIFICATE")
                .eq(ContentRelation::getIsDel, 0);
        ContentRelation existingRelation = contentRelationMapper.selectOne(queryWrapper);

        if (existingRelation != null) {
            // 如果证书ID发生变化，更新现有关联
            if (!existingRelation.getContentId().equals(request.getCertificateId())) {
                existingRelation.setContentId(request.getCertificateId());
                existingRelation.setUpdaterId(userId);
                existingRelation.setUpdaterName(userName);
                existingRelation.setGmtModified(now);
                contentRelationMapper.updateById(existingRelation);
            }
        } else {
            // 创建新的证书关联
            ContentRelation certificateRelation = new ContentRelation();
            certificateRelation.setBizId(trainId);
            certificateRelation.setBizType(BizType.TRAIN);
            certificateRelation.setContentType("CERTIFICATE");
            certificateRelation.setContentBizType("CERTIFICATE");
            certificateRelation.setContentId(request.getCertificateId());
            certificateRelation.setSortOrder(0);
            certificateRelation.setIsRequired(1);
            certificateRelation.setCreatorId(userId);
            certificateRelation.setCreatorName(userName);
            certificateRelation.setUpdaterId(userId);
            certificateRelation.setUpdaterName(userName);
            certificateRelation.setGmtCreate(now);
            certificateRelation.setGmtModified(now);
            certificateRelation.setIsDel(0);
            contentRelationMapper.insert(certificateRelation);
        }
    }


    /**
     * 解析urlKey为url
     *
     * @param urlKey urlKey
     * @return url
     */
    private String parseUrlKey(String urlKey) {
        if (!StringUtils.hasText(urlKey)) {
            return null;
        }
        try {
            return fileService.getUrl(urlKey).getUrl();
        } catch (Exception e) {
            log.error("解析urlKey失败: {}", urlKey, e);
            return urlKey;
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCreator(List<Long> tranIds, Long newCreatorId, String newCreatorName, Long operatorId, String operatorName) {
        log.info("批量更新培训创建人，课程ID列表：{}，新创建人ID：{}，操作人：{}", tranIds, newCreatorId, operatorName);
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(tranIds)) {
            return false;
        }

        // 执行批量更新
        trainMapper.batchUpdateCreator(tranIds, newCreatorId, newCreatorName, operatorId, operatorName);
        return true;
    }
}

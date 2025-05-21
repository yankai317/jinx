package com.learn.service.train.impl;

import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.dto.train.TrainCreateRequest;
import com.learn.dto.train.TrainDetailDTO;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.FileService;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeCreateResponse;
import com.learn.service.train.TrainCreateService;
import com.learn.service.train.TrainQueryService;
import com.learn.service.user.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 培训创建服务实现类
 */
@Service
@Slf4j
public class TrainCreateServiceImpl implements TrainCreateService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired
    private FileService fileService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private TrainQueryService trainQueryService;

    @Autowired
    private RangeSetService rangeSetService;

    /**
     * 创建培训
     *
     * @param request 创建培训请求
     * @return 创建的培训详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TrainDetailDTO createTrain(TrainCreateRequest request, UserInfoResponse userInfo) {
        // 参数校验
        if (request == null) {
            throw new CommonException("请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new CommonException("培训名称不能为空");
        }

        // 获取当前用户信息
        Long userId = userInfo.getUserId();
        String userName = userInfo.getNickname();

        // 创建培训基本信息
        Train train = buildTrainEntity(request, userId, userName);
        trainMapper.insert(train);
        Long trainId = train.getId();

        // 关联培训内容
        if (!CollectionUtils.isEmpty(request.getContents())) {
            List<ContentRelation> contentRelations = buildTrainContentRelations(request.getContents(), trainId, userId, userName);
            for (ContentRelation relation : contentRelations) {
                contentRelationMapper.insert(relation);
            }
        }
        
        // 关联证书内容
        if (request.getCertificateId() != null) {
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
            certificateRelation.setGmtCreate(new Date());
            certificateRelation.setGmtModified(new Date());
            certificateRelation.setIsDel(0);
            contentRelationMapper.insert(certificateRelation);
        }

        // 设置培训可见范围
        request.setOperatorId(userId);
        request.setOperatorName(userName);
        rangeSetService.setVisibilityRange(BizType.TRAIN, trainId, request);

        // 设置培训协同管理
        rangeSetService.setCollaborators(BizType.TRAIN, trainId, request);

        // 查询并返回创建的培训详情
        return trainQueryService.getTrainDetail(trainId);
    }

    /**
     * 构建培训实体
     *
     * @param request  创建培训请求
     * @param userId   用户ID
     * @param userName 用户名称
     * @return 培训实体
     */
    private Train buildTrainEntity(TrainCreateRequest request, Long userId, String userName) {
        Train train = new Train();
        train.setName(request.getTitle());

        // 处理封面图URL，将urlKey解析为url
        if (StringUtils.hasText(request.getCover())) {
            train.setCover(parseUrlKey(request.getCover()));
        }

        train.setIntroduction(request.getIntroduction());
        train.setCredit(request.getCredit() != null ? request.getCredit() : 0);

        // 处理分类IDs
        if (!org.apache.commons.lang3.StringUtils.isBlank(request.getCategoryIds())) {
            train.setCategoryIds(request.getCategoryIds());
        }

        // 处理是否允许评论
        train.setAllowComment(request.getAllowComment() != null ? (request.getAllowComment() ? 1 : 0) : 1);

        train.setCertificateId(request.getCertificateId());
        train.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "draft");
        train.setCreatorId(userId);
        train.setCreatorName(userName);
        train.setUpdaterId(userId);
        train.setUpdaterName(userName);
        train.setGmtCreate(new Date());
        train.setGmtModified(new Date());
        train.setIsDel(0);
        train.setIfIsCitable(request.getIfIsCitable());

        return train;
    }

    /**
     * 构建培训内容关联实体列表
     *
     * @param contents 培训内容请求列表
     * @param trainId  培训ID
     * @param userId   用户ID
     * @param userName 用户名称
     * @return 培训内容关联实体列表
     */
    private List<ContentRelation> buildTrainContentRelations(List<TrainCreateRequest.TrainContentRequest> contents, Long trainId, Long userId, String userName) {
        List<ContentRelation> relations = new ArrayList<>();
        for (TrainCreateRequest.TrainContentRequest content : contents) {
            ContentRelation relation = new ContentRelation();
            relation.setBizId(trainId);
            relation.setBizType(BizType.TRAIN);
            relation.setContentType(content.getType());
            relation.setContentId(content.getContentId());
            relation.setContentBizType(content.getSubType());
            relation.setSortOrder(content.getSortOrder() != null ? content.getSortOrder() : 0);
            relation.setIsRequired(content.getIsRequired() != null ? (content.getIsRequired() ? 1 : 0) : 1);
            relation.setCreatorId(userId);
            relation.setCreatorName(userName);
            relation.setUpdaterId(userId);
            relation.setUpdaterName(userName);
            relation.setGmtCreate(new Date());
            relation.setGmtModified(new Date());
            relation.setIsDel(0);
            relations.add(relation);
        }
        return relations;
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
}

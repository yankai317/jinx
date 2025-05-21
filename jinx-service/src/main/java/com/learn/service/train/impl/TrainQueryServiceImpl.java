package com.learn.service.train.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.train.TrainDetailDTO;
import com.learn.dto.train.TrainListDTO;
import com.learn.dto.train.TrainListRequest;
import com.learn.dto.train.TrainListResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.CategoryMapper;
import com.learn.infrastructure.repository.mapper.CertificateMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.category.CategoryQueryService;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.train.TrainQueryService;
import com.learn.util.JinxDateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 培训查询服务实现类
 */
@Service
@Slf4j
public class TrainQueryServiceImpl implements TrainQueryService {

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;
    
    @Autowired
    private ContentRelationMapper contentRelationMapper;
    
    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private RangeSetService rangeSetService;
    @Autowired
    private CategoryQueryService categoryQueryService;

    @Override
    public TrainListResponse getTrainList(TrainListRequest request) {
        log.info("获取培训列表请求参数: {}", request);

        // 1. 判断数据可见范围
        List<Long> visibleTrainIds = getVisibleTrainIds(request);
        
        // 2. 构建查询条件
        LambdaQueryWrapper<Train> queryWrapper = buildQueryWrapper(request, visibleTrainIds);
        
        // 3. 分页查询
        IPage<Train> page = new Page<>(request.getPageNum(), request.getPageSize());
        page = trainMapper.selectPage(page, queryWrapper);
        
        // 4. 转换为DTO
        List<TrainListDTO> trainListDTOs = convertToTrainListDTOs(page.getRecords());
        
        // 5. 构建响应
        TrainListResponse response = new TrainListResponse();
        response.setTotal((int) page.getTotal());
        response.setList(trainListDTOs);
        
        return response;
    }
    
    @Override
    public TrainDetailDTO getTrainDetail(Long id) {
        log.info("获取培训详情，培训ID: {}", id);
        
        // 1. 获取培训基本信息
        Train train = trainMapper.selectById(id);
        if (train == null || train.getIsDel() == 1) {
            log.error("培训不存在或已删除，培训ID: {}", id);
            return null;
        }
        
        // 2. 构建培训详情DTO
        TrainDetailDTO detailDTO = new TrainDetailDTO();
        detailDTO.setId(train.getId());
        detailDTO.setTitle(train.getName());
        detailDTO.setCover(train.getCover());
        detailDTO.setIntroduction(train.getIntroduction());
        detailDTO.setCredit(train.getCredit());
        detailDTO.setAllowComment(train.getAllowComment() != null && train.getAllowComment() == 1);
        detailDTO.setCertificateId(train.getCertificateId());
        detailDTO.setStatus(train.getStatus());
        detailDTO.setCreatorId(train.getCreatorId());
        detailDTO.setCreatorName(train.getCreatorName());
        detailDTO.setGmtCreate(train.getGmtCreate());
        detailDTO.setIfIsCitable(train.getIfIsCitable());
        
        // 3. 获取分类信息
        setCategoryInfo(detailDTO, train);
        
        // 4. 获取证书信息
        setCertificateInfo(detailDTO, train);
        
        // 5. 获取培训内容列表
        setContentInfo(detailDTO, train);
        
        // 6. 获取可见范围信息
        detailDTO.setVisibility(rangeSetService.processVisibilityInfo(BizType.TRAIN, train.getId()));

        // 7. 获取协同管理信息
        detailDTO.setCollaborators(rangeSetService.processCollaboratorsInfo(BizType.TRAIN, train.getId()));
        
        return detailDTO;
    }

    /**
     * 获取用户可见的培训ID列表
     * @param request 请求参数
     * @return 可见的培训ID列表
     */
    private List<Long> getVisibleTrainIds(TrainListRequest request) {
        // 构建范围查询请求
        CommonRangeQueryRequest rangeRequest = new CommonRangeQueryRequest();
        rangeRequest.setModelType("VISIBLE"); // 可见范围功能
        rangeRequest.setType(BizType.TRAIN); // 培训业务类型
        rangeRequest.setTargetTypeAndIds(request.getTargetTypeAndIds());


        // 如果只看我创建的，则直接返回我创建的培训ID列表
        if (Boolean.TRUE.equals(request.getOnlyMine()) && request.getCreatorId() != null) {
            LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Train::getCreatorId, request.getCreatorId()).eq(Train::getIsDel, 0);
            List<Train> myTrains = trainMapper.selectList(wrapper);
            return myTrains.stream().map(Train::getId).collect(Collectors.toList());
        }
        
        // 调用公共范围接口查询可见范围
        List<CommonRangeQueryResponse> responses = commonRangeInterface.queryBusinessIdsByTargets(rangeRequest);
        
        if (CollectionUtils.isEmpty(responses)) {
            return Collections.emptyList();
        }
        
        // 提取业务ID列表
        return responses.stream()
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
    }

    /**
     * 构建查询条件
     * @param request 请求参数
     * @param visibleTrainIds 可见的培训ID列表
     * @return 查询条件
     */
    private LambdaQueryWrapper<Train> buildQueryWrapper(TrainListRequest request, List<Long> visibleTrainIds) {
        LambdaQueryWrapper<Train> queryWrapper = new LambdaQueryWrapper<>();
        
        // 设置可见范围条件
        if (!CollectionUtils.isEmpty(visibleTrainIds)) {
            queryWrapper.in(Train::getId, visibleTrainIds);
        }
        
        // 设置基本条件
        queryWrapper.eq(Train::getIsDel, 0); // 未删除
        
        // 设置查询条件
        if (StringUtils.isNotBlank(request.getTitle())) {
            queryWrapper.like(Train::getName, request.getTitle());
        }

        // 引用
        if (Objects.nonNull(request.getIfIsCitable())) {
            queryWrapper.eq(Train::getIfIsCitable, request.getIfIsCitable());
        }
        
        if (StringUtils.isNotBlank(request.getStatus())) {
            queryWrapper.eq(Train::getStatus, request.getStatus());
        }
        
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(request.getCategoryIds())) {
            List<Long> categoryIds = categoryQueryService.fullSearchCategoryId(request.getCategoryIds());
            queryWrapper.in(Train::getCategoryIds, categoryIds);
        }
        
        if (request.getCreatorId() != null) {
            queryWrapper.eq(Train::getCreatorId, request.getCreatorId());
        }
        
        // 设置时间范围
        if (StringUtils.isNotBlank(request.getStartTime())) {
            try {
                Date startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.getStartTime());
                queryWrapper.ge(Train::getGmtCreate, startDate);
            } catch (ParseException e) {
                log.error("解析开始时间失败", e);
            }
        }
        
        if (StringUtils.isNotBlank(request.getEndTime())) {
            try {
                Date endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.getEndTime());
                queryWrapper.le(Train::getGmtCreate, endDate);
            } catch (ParseException e) {
                log.error("解析结束时间失败", e);
            }
        }
        
        // 设置排序
        if (StringUtils.isNotBlank(request.getSortField())) {
            String sortField = request.getSortField();
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            
            switch (sortField) {
                case "gmt_create":
                    queryWrapper.orderBy(true, isAsc, Train::getGmtCreate);
                    break;
                case "name":
                    queryWrapper.orderBy(true, isAsc, Train::getName);
                    break;
                case "credit":
                    queryWrapper.orderBy(true, isAsc, Train::getCredit);
                    break;
                default:
                    queryWrapper.orderByDesc(Train::getGmtCreate); // 默认按创建时间降序
            }
        } else {
            queryWrapper.orderByDesc(Train::getGmtCreate); // 默认按创建时间降序
        }
        
        return queryWrapper;
    }

    /**
     * 将Train实体列表转换为TrainListDTO列表
     * @param trains Train实体列表
     * @return TrainListDTO列表
     */
    private List<TrainListDTO> convertToTrainListDTOs(List<Train> trains) {
        if (CollectionUtils.isEmpty(trains)) {
            return Collections.emptyList();
        }
        
        // 获取所有培训ID
        List<Long> trainIds = trains.stream().map(Train::getId).collect(Collectors.toList());
        
        // 获取所有证书ID
        List<Long> certificateIds = trains.stream()
                .map(Train::getCertificateId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询证书信息
        Map<Long, Certificate> certificateMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(certificateIds)) {
            LambdaQueryWrapper<Certificate> certificateWrapper = new LambdaQueryWrapper<>();
            certificateWrapper.in(Certificate::getId, certificateIds).eq(Certificate::getIsDel, 0);
            List<Certificate> certificates = certificateMapper.selectList(certificateWrapper);
            certificateMap = certificates.stream().collect(Collectors.toMap(Certificate::getId, c -> c));
        }
        
        // 批量查询分类信息
        Map<Long, Category> categoryMap = new HashMap<>();
        LambdaQueryWrapper<Category> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(Category::getIsDel, 0);
        List<Category> categories = categoryMapper.selectList(categoryWrapper);
        categoryMap = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
        
        // 批量查询学习完成信息
        Map<Long, List<UserLearningTask>> completionMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(trainIds)) {
            LambdaQueryWrapper<UserLearningTask> completionWrapper = new LambdaQueryWrapper<>();
            completionWrapper.in(UserLearningTask::getBizId, trainIds)
                            .eq(UserLearningTask::getBizType, BizType.TRAIN)
                            .eq(UserLearningTask::getIsDel, 0);
            List<UserLearningTask> completions = userLearningTaskMapper.selectList(completionWrapper);
            completionMap = completions.stream().collect(Collectors.groupingBy(UserLearningTask::getBizId));
        }
        
        // 转换为DTO
        List<TrainListDTO> result = new ArrayList<>();
        for (Train train : trains) {
            TrainListDTO dto = new TrainListDTO();
            dto.setId(train.getId());
            dto.setTitle(train.getName());
            dto.setCover(train.getCover());
            dto.setCredit(train.getCredit());
            dto.setStatus(train.getStatus());
            dto.setCertificateId(train.getCertificateId());
            dto.setCreatorName(train.getCreatorName());
            dto.setGmtCreate(JinxDateUtil.formatDateToString(train.getGmtCreate()));
            dto.setGmtModified(JinxDateUtil.formatDateToString(train.getGmtModified()));
            // 设置证书名称
            if (train.getCertificateId() != null && certificateMap.containsKey(train.getCertificateId())) {
                dto.setCertificateName(certificateMap.get(train.getCertificateId()).getName());
            }
            
            // 设置分类名称列表
            List<String> categoryNames = new ArrayList<>();
            dto.setCategoryIds(train.getCategoryIds());
            if (StringUtils.isNotBlank(train.getCategoryIds())) {
                String[] categoryIdArray = train.getCategoryIds().split(",");
                for (String categoryIdStr : categoryIdArray) {
                    try {
                        Long categoryId = Long.parseLong(categoryIdStr);
                        if (categoryMap.containsKey(categoryId)) {
                            categoryNames.add(categoryMap.get(categoryId).getName());
                        }
                    } catch (NumberFormatException e) {
                        log.error("解析分类ID失败: {}", categoryIdStr, e);
                    }
                }
            }
            dto.setCategoryNames(categoryNames);
            
            // 设置学习人数和完成人数
            List<UserLearningTask> completions = completionMap.getOrDefault(train.getId(), Collections.emptyList());
            dto.setLearnerCount(completions.size());
            dto.setCompletionCount((int) completions.stream()
                    .filter(c -> c.getStatus() != null && c.getStatus().equals(LearningStatus.COMPLETED)) // 状态2表示已完成
                    .count());
            
            result.add(dto);
        }
        
        return result;
    }
    
    /**
     * 设置分类信息
     * @param detailDTO 培训详情DTO
     * @param train 培训实体
     */
    private void setCategoryInfo(TrainDetailDTO detailDTO, Train train) {
        List<Long> categoryIds = new ArrayList<>();
        List<String> categoryNames = new ArrayList<>();
        
        if (StringUtils.isNotBlank(train.getCategoryIds())) {
            String[] categoryIdArray = train.getCategoryIds().split(",");
            for (String categoryIdStr : categoryIdArray) {
                try {
                    Long categoryId = Long.parseLong(categoryIdStr);
                    categoryIds.add(categoryId);
                    // 查询分类名称
                    Category category = categoryMapper.selectById(categoryId);
                    if (category != null && category.getIsDel() == 0) {
                        categoryNames.add(category.getName());
                    }
                } catch (NumberFormatException e) {
                    log.error("解析分类ID失败: {}", categoryIdStr, e);
                }
            }
        }
        
        detailDTO.setCategoryIds(train.getCategoryIds());
        detailDTO.setCategoryNames(categoryNames);
    }
    
    /**
     * 设置证书信息
     * @param detailDTO 培训详情DTO
     * @param train 培训实体
     */
    private void setCertificateInfo(TrainDetailDTO detailDTO, Train train) {
        if (train.getCertificateId() != null) {
            Certificate certificate = certificateMapper.selectById(train.getCertificateId());
            if (certificate != null && certificate.getIsDel() == 0) {
                detailDTO.setCertificateName(certificate.getName());
            }
        }
    }
    
    /**
     * 设置培训内容列表
     * @param detailDTO 培训详情DTO
     * @param train 培训实体
     */
    private void setContentInfo(TrainDetailDTO detailDTO, Train train) {
        // 查询培训内容关联
        LambdaQueryWrapper<ContentRelation> contentWrapper = new LambdaQueryWrapper<>();
        contentWrapper.eq(ContentRelation::getBizId, train.getId()).eq(ContentRelation::getBizType, BizType.TRAIN)
                     .eq(ContentRelation::getIsDel, 0)
                     .orderByAsc(ContentRelation::getSortOrder);
        List<ContentRelation> contentRelations = contentRelationMapper.selectList(contentWrapper);
        
        if (CollectionUtils.isEmpty(contentRelations)) {
            detailDTO.setContents(Collections.emptyList());return;
        }
        
        // 获取所有内容ID
        List<Long> contentIds = contentRelations.stream()
                .filter(relation -> BizType.COURSE.equals(relation.getContentType()))
                .map(ContentRelation::getContentId)
                .collect(Collectors.toList());// 批量查询课程信息
        Map<Long, Courses> coursesMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(contentIds)) {
            LambdaQueryWrapper<Courses> coursesWrapper = new LambdaQueryWrapper<>();
            coursesWrapper.in(Courses::getId, contentIds).eq(Courses::getIsDel, 0);
            List<Courses> coursesList = coursesMapper.selectList(coursesWrapper);
            coursesMap = coursesList.stream().collect(Collectors.toMap(Courses::getId, c -> c));
        }
        
        // 转换为DTO
        List<TrainDetailDTO.TrainContentDTO> contentDTOs = new ArrayList<>();
        for (ContentRelation relation : contentRelations) {
            if (BizType.CERTIFICATE.equals(relation.getContentBizType())) {
                continue;
            }
            TrainDetailDTO.TrainContentDTO contentDTO = new TrainDetailDTO.TrainContentDTO();
            contentDTO.setId(relation.getId());
            contentDTO.setType(relation.getContentType());
            contentDTO.setContentId(relation.getContentId());
            contentDTO.setContentUrl(relation.getContentUrl());
            contentDTO.setIsRequired(relation.getIsRequired() != null && relation.getIsRequired() == 1);
            contentDTO.setSortOrder(relation.getSortOrder());
            
            // 设置内容标题和类型
            if (BizType.COURSE.equals(relation.getContentType()) && relation.getContentId() != null) {
                Courses course = coursesMap.get(relation.getContentId());
                if (course != null) {
                    contentDTO.setTitle(course.getTitle());
                    contentDTO.setContentType(course.getType());
                }
            } else {
                // 对于非课程类型的内容，可以从属性中获取标题
                contentDTO.setTitle(relation.getContentType());
            }
            
            contentDTOs.add(contentDTO);
        }
        
        detailDTO.setContents(contentDTOs);
    }

}

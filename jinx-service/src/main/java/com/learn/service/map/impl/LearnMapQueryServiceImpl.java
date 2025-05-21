package com.learn.service.map.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.map.LearningMapDTO;
import com.learn.dto.map.LearningMapDetailDTO;
import com.learn.dto.map.LearningMapListRequest;
import com.learn.dto.map.LearningMapListResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.CommonRangeInterface;
import com.learn.service.FileService;
import com.learn.service.category.CategoryQueryService;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.map.LearnMapQueryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习地图查询服务实现类
 */
@Service
@Slf4j
public class LearnMapQueryServiceImpl implements LearnMapQueryService {

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CertificateMapper certificateMapper;

    @Autowired
    private LearningMapStageMapper learningMapStageMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Autowired
    private RangeSetService rangeSetService;

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private TrainMapper trainMapper;
    @Autowired
    private CategoryQueryService categoryQueryService;

    /**
     * 获取学习地图列表
     *
     * @param request 查询请求
     * @param userId 当前用户ID
     * @return 学习地图列表响应
     */
    @Override
    public LearningMapListResponse getLearningMapList(LearningMapListRequest request, Long userId) {
        log.info("获取学习地图列表，请求参数：{}, 用户ID：{}", request, userId);
        // 构建查询条件
        LambdaQueryWrapper<LearningMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningMap::getIsDel, 0); // 未删除的记录

        // 名称模糊查询
        if (StringUtils.isNotBlank(request.getName())) {
            queryWrapper.like(LearningMap::getName, request.getName());
        }

        // 分类ID查询
        if (CollectionUtils.isNotEmpty(request.getCategoryIds())) {
            List<Long> categoryIds = categoryQueryService.fullSearchCategoryId(request.getCategoryIds());
            queryWrapper.in(LearningMap::getCategoryIds, categoryIds);
        }

        // 创建人ID查询
        if (request.getCreatorId() != null) {
            queryWrapper.eq(LearningMap::getCreatorId, request.getCreatorId());
        }

        // 只看我创建的
        if (request.getOnlyMine() != null && request.getOnlyMine() && userId != null) {
            queryWrapper.eq(LearningMap::getCreatorId, userId);
        }

        // 创建时间范围查询
        if (StringUtils.isNotBlank(request.getStartTime())) {
            queryWrapper.ge(LearningMap::getGmtCreate, request.getStartTime());
        }
        if (StringUtils.isNotBlank(request.getEndTime())) {
            queryWrapper.le(LearningMap::getGmtCreate, request.getEndTime());
        }
        // 排序
        if (StringUtils.isNotBlank(request.getSortField())) {
            String sortField = request.getSortField();
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());

            // 根据排序字段进行排序
            switch (sortField) {
                case "gmt_create":
                    queryWrapper.orderBy(true, isAsc, LearningMap::getGmtCreate);
                    break;
                case "name":
                    queryWrapper.orderBy(true, isAsc, LearningMap::getName);
                    break;
                default:
                    queryWrapper.orderByDesc(LearningMap::getGmtCreate); // 默认按创建时间降序
            }
        } else {
            queryWrapper.orderByDesc(LearningMap::getGmtCreate); // 默认按创建时间降序
        }
        // 分页查询
        Page<LearningMap> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<LearningMap> mapPage = learningMapMapper.selectPage(page, queryWrapper);

        // 判断数据可见范围
        List<LearningMap> visibleMaps = filterVisibleMaps(mapPage.getRecords(), userId);

        // 构建响应对象
        LearningMapListResponse response = new LearningMapListResponse();
        response.setTotal((int) mapPage.getTotal());
        if (visibleMaps.isEmpty()) {
            response.setList(new ArrayList<>());
            return response;
        }

        // 获取所有相关的分类ID
        List<Long> allCategoryIds = extractAllCategoryIds(visibleMaps);

        // 批量查询分类信息
        Map<Long, Category> categoryMap = batchQueryCategories(allCategoryIds);// 批量查询证书信息
        List<Long> certificateIds = visibleMaps.stream()
                .map(LearningMap::getCertificateId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());
        Map<Long, Certificate> certificateMap = batchQueryCertificates(certificateIds);

        // 批量查询阶段数量
        Map<Long, Integer> stageCountMap = batchQueryStageCount(visibleMaps.stream()
                .map(LearningMap::getId)
                .collect(Collectors.toList()));// 批量查询学习人数和完成人数
        Map<Long, Integer> learnerCountMap = new HashMap<>();
        Map<Long, Integer> completionCountMap = new HashMap<>();
        batchQueryLearnerAndCompletionCount(visibleMaps.stream()
                .map(LearningMap::getId)
                .collect(Collectors.toList()), learnerCountMap, completionCountMap);

        // 转换为DTO
        List<LearningMapDTO> dtoList = visibleMaps.stream()
                .map(map -> convertToDTO(map, categoryMap, certificateMap, stageCountMap,
                        learnerCountMap, completionCountMap))
                .collect(Collectors.toList());
        response.setList(dtoList);
        return response;
    }

    /**
     * 获取学习地图详情
     *
     * @param id 学习地图ID
     * @param userId 当前用户ID
     * @return 学习地图详情
     */
    @Override
    public LearningMapDetailDTO getLearningMapDetail(Long id, Long userId) {
        log.info("获取学习地图详情，地图ID：{}, 用户ID：{}", id, userId);// 参数校验
        if (id == null || id <= 0) {
            log.error("学习地图ID无效：{}", id);
            return null;
        }

        // 查询学习地图基本信息
        LearningMap learningMap = learningMapMapper.selectById(id);
        if (learningMap == null || learningMap.getIsDel() == 1) {
            log.error("学习地图不存在或已删除，ID：{}", id);
            return null;
        }

        //todo 后续判断用户是否有权限访问该学习地图
//        if (!checkUserHasPermission(id, userId)) {
//            log.error("用户无权访问该学习地图，用户ID：{}，地图ID：{}", userId, id);
//            return null;
//        }

        // 构建学习地图详情DTO
        LearningMapDetailDTO detailDTO = new LearningMapDetailDTO();
        // 设置基本信息
        detailDTO.setId(learningMap.getId());
        detailDTO.setName(learningMap.getName());
        detailDTO.setCover(learningMap.getCover());
        detailDTO.setIntroduction(learningMap.getIntroduction());
        detailDTO.setCreditRule(learningMap.getCreditRule());
        detailDTO.setRequiredCredit(learningMap.getRequiredCredit());
        detailDTO.setElectiveCredit(learningMap.getElectiveCredit());
        detailDTO.setCertificateRule(learningMap.getCertificateRule());
        detailDTO.setCertificateId(learningMap.getCertificateId());
        detailDTO.setDingtalkGroup(learningMap.getDingtalkGroup());
        detailDTO.setDingtalkGroupId(learningMap.getDingtalkGroupId());
        detailDTO.setUnlockMode(learningMap.getUnlockMode());
        detailDTO.setTheme(learningMap.getTheme());
        detailDTO.setCreatorId(learningMap.getCreatorId());
        detailDTO.setCreatorName(learningMap.getCreatorName());
        // 设置时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (learningMap.getStartTime() != null) {
            detailDTO.setStartTime(sdf.format(learningMap.getStartTime()));
        }
        if (learningMap.getEndTime() != null) {
            detailDTO.setEndTime(sdf.format(learningMap.getEndTime()));
        }

        // 设置分类信息
        setCategoryInfo(detailDTO, learningMap.getCategoryIds());

        // 设置证书信息
        setCertificateInfo(detailDTO, learningMap.getCertificateId());

        // 设置阶段和任务信息
        setStagesAndTasks(detailDTO, id);// 设置可见范围信息


        // 处理可见范围信息
        detailDTO.setVisibility(rangeSetService.processVisibilityInfo(BizType.LEARNING_MAP, learningMap.getId()));

        // 处理协同管理信息
        detailDTO.setCollaborators(rangeSetService.processCollaboratorsInfo(BizType.LEARNING_MAP, learningMap.getId()));

        return detailDTO;
    }
/**
     * 设置分类信息
     *
     * @param detailDTO 学习地图详情DTO
     * @param categoryIdsStr 分类ID字符串
     */
    private void setCategoryInfo(LearningMapDetailDTO detailDTO, String categoryIdsStr) {
        if (StringUtils.isBlank(categoryIdsStr)) {
            detailDTO.setCategoryIds("");
            detailDTO.setCategoryNames(new ArrayList<>());
            return;
        }

        // 解析分类ID
        List<Long> categoryIds = new ArrayList<>();
        for (String idStr : categoryIdsStr.split(",")) {
            try {
                Long categoryId = Long.parseLong(idStr.trim());
                categoryIds.add(categoryId);
            } catch (NumberFormatException e) {
                log.warn("解析分类ID失败: {}", idStr);
            }
        }
        detailDTO.setCategoryIds(categoryIdsStr);
        if (categoryIds.isEmpty()) {
            detailDTO.setCategoryNames(new ArrayList<>());
            return;
        }

        // 查询分类信息
        Map<Long, Category> categoryMap = batchQueryCategories(categoryIds);

        // 设置分类名称
        List<String> categoryNames = Lists.newArrayList(categoryMap.getOrDefault(categoryIds, new Category()).getName());
        detailDTO.setCategoryNames(categoryNames);
    }/**
     * 设置证书信息
     *
     * @param detailDTO 学习地图详情DTO
     * @param certificateId 证书ID
     */
    private void setCertificateInfo(LearningMapDetailDTO detailDTO, Long certificateId) {
        if (certificateId == null || certificateId <= 0) {
            return;
        }// 查询证书信息
        Certificate certificate = certificateMapper.selectById(certificateId);
        if (certificate != null && certificate.getIsDel() == 0) {
            detailDTO.setCertificateName(certificate.getName());
        }
    }/**
     * 设置阶段和任务信息
     *
     * @param detailDTO 学习地图详情DTO
     * @param mapId 学习地图ID
     */
    private void setStagesAndTasks(LearningMapDetailDTO detailDTO, Long mapId) {
        // 查询阶段信息
        LambdaQueryWrapper<LearningMapStage> stageQueryWrapper = new LambdaQueryWrapper<>();
        stageQueryWrapper.eq(LearningMapStage::getMapId, mapId);
        stageQueryWrapper.eq(LearningMapStage::getIsDel, 0);
        stageQueryWrapper.orderByAsc(LearningMapStage::getStageOrder);
        List<LearningMapStage> stages = learningMapStageMapper.selectList(stageQueryWrapper);

        if (stages.isEmpty()) {
            detailDTO.setStages(new ArrayList<>());
            return;
        }

        // 获取所有阶段ID
        List<Long> stageIds = stages.stream()
                .map(LearningMapStage::getId)
                .collect(Collectors.toList());// 获取所有阶段的证书ID
        List<Long> stageCertificateIds = stages.stream()
                .map(LearningMapStage::getCertificateId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toList());

        // 批量查询证书信息
        Map<Long, Certificate> certificateMap = batchQueryCertificates(stageCertificateIds);

        // 批量查询任务信息
        Map<Long, List<ContentRelation>> stageTasksMap = batchQueryStageTasks(stageIds);// 转换为DTO
        List<LearningMapDetailDTO.StageDTO> stageDTOs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (LearningMapStage stage : stages) {
            LearningMapDetailDTO.StageDTO stageDTO = new LearningMapDetailDTO.StageDTO();
            stageDTO.setId(stage.getId());
            stageDTO.setName(stage.getName());
            stageDTO.setStageOrder(stage.getStageOrder());
            stageDTO.setOpenType(stage.getOpenType());
            stageDTO.setDurationDays(stage.getDurationDays());
            stageDTO.setCredit(stage.getCredit());
            stageDTO.setCertificateId(stage.getCertificateId());

            // 设置时间
            if (stage.getStartTime() != null) {
                stageDTO.setStartTime(sdf.format(stage.getStartTime()));
            }
            if (stage.getEndTime() != null) {
                stageDTO.setEndTime(sdf.format(stage.getEndTime()));
            }

            // 设置证书名称
            if (stage.getCertificateId() != null && certificateMap.containsKey(stage.getCertificateId())) {
                stageDTO.setCertificateName(certificateMap.get(stage.getCertificateId()).getName());
            }

            // 设置任务列表
            List<ContentRelation> tasks = stageTasksMap.getOrDefault(stage.getId(), new ArrayList<>());
            stageDTO.setTasks(convertToTaskDTOs(tasks));

            stageDTOs.add(stageDTO);
        }detailDTO.setStages(stageDTOs);
    }/**
     * 将任务关联实体转换为任务DTO
     *
     * @param tasks 任务关联实体列表
     * @return 任务DTO列表
     */
    private List<LearningMapDetailDTO.TaskDTO> convertToTaskDTOs(List<ContentRelation> tasks) {
        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }
        return tasks.stream()
                .map(task -> {
                    LearningMapDetailDTO.TaskDTO taskDTO = new LearningMapDetailDTO.TaskDTO();
                    taskDTO.setId(task.getId());
                    taskDTO.setType(task.getContentType());
                    taskDTO.setContentId(task.getContentId());
                    taskDTO.setTitle(getContentTitle(task));
                    taskDTO.setContentType(task.getContentBizType());
                    taskDTO.setContentUrl(task.getContentUrl());
                    taskDTO.setIsRequired(task.getIsRequired() == 1);
                    taskDTO.setSortOrder(task.getSortOrder());
                    return taskDTO;
                })
                .collect(Collectors.toList());
    }
    /**
     * 获取内容标题
     *
     * @param task 任务关联实体
     * @return 内容标题
     */
    private String getContentTitle(ContentRelation task) {
        // 根据contentType区分课程和培训
        String contentType = task.getContentType();
        Long contentId = task.getContentId();
        
        if (contentId == null || contentId <= 0) {
            return "";
        }
        
        // 根据contentType查询对应类型的内容
        if (BizType.COURSE.equalsIgnoreCase(contentType)) {
            // 查询课程信息
            Courses course = coursesMapper.selectById(contentId);
            if (course != null && course.getIsDel() == 0) {
                return course.getTitle(); // 返回课程标题
            }
        } else if (BizType.TRAIN.equalsIgnoreCase(contentType)) {
            // 查询培训信息
            Train train = trainMapper.selectById(contentId);
            if (train != null && train.getIsDel() == 0) {
                return train.getName(); // 返回培训名称
            }
        }
        
        return "";
    }

    /**
     * 批量查询阶段任务
     *
     * @param stageIds 阶段ID列表
     * @return阶段ID到任务列表的映射
     */
    private Map<Long, List<ContentRelation>> batchQueryStageTasks(List<Long> stageIds) {
        if (stageIds == null || stageIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, List<ContentRelation>> stageTasksMap = new HashMap<>();LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ContentRelation::getBizId, stageIds);
        queryWrapper.eq(ContentRelation::getBizType, BizType.MAP_STAGE); // 地图阶段
        queryWrapper.notIn(ContentRelation::getContentType,Lists.newArrayList(BizType.CERTIFICATE)); // 地图阶段
        queryWrapper.eq(ContentRelation::getIsDel, 0); // 未删除的记录
        queryWrapper.orderByAsc(ContentRelation::getSortOrder);
        List<ContentRelation> tasks = contentRelationMapper.selectList(queryWrapper);

        // 按阶段ID分组
        for (ContentRelation task : tasks) {
            Long stageId = task.getBizId();
            if (!stageTasksMap.containsKey(stageId)) {
                stageTasksMap.put(stageId, new ArrayList<>());
            }
            stageTasksMap.get(stageId).add(task);
        }

        return stageTasksMap;
    }

    /**
     * 过滤出用户可见的学习地图
     *
     * @param maps 学习地图列表
     * @param userId 用户ID
     * @return 可见的学习地图列表
     */
    private List<LearningMap> filterVisibleMaps(List<LearningMap> maps, Long userId) {
        if (maps == null || maps.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取所有地图ID
        List<Long> mapIds = maps.stream()
                .map(LearningMap::getId)
                .collect(Collectors.toList());

        // 构建范围查询请求
        CommonRangeQueryRequest rangeRequest = new CommonRangeQueryRequest();
        rangeRequest.setModelType("VISIBLE"); // 可见范围
        rangeRequest.setType(BizType.LEARNING_MAP); // 学习地图
        rangeRequest.setUserId(userId.toString());
        // todo
        Boolean hasRight = true;
//        Boolean hasRight = commonRangeInterface.checkUserHasRight(rangeRequest);

        if (Boolean.TRUE.equals(hasRight)) {
            return maps;
        } else {
            // 如果没有权限，则返回空列表
            return new ArrayList<>();
        }
    }/**
     * 提取所有分类ID
     *
     * @param maps 学习地图列表
     * @return 所有分类ID列表
     */
    private List<Long> extractAllCategoryIds(List<LearningMap> maps) {
        List<Long> allCategoryIds = new ArrayList<>();

        for (LearningMap map : maps) {
            String categoryIdsStr = map.getCategoryIds();
            if (StringUtils.isNotBlank(categoryIdsStr)) {
                String[] categoryIdArray = categoryIdsStr.split(",");
                for (String categoryIdStr : categoryIdArray) {
                    try {
                        Long categoryId = Long.parseLong(categoryIdStr.trim());
                        allCategoryIds.add(categoryId);
                    } catch (NumberFormatException e) {
                        log.warn("解析分类ID失败: {}", categoryIdStr);
                    }
                }
            }
        }return allCategoryIds;
    }

    /**
     * 批量查询分类信息
     *
     * @param categoryIds 分类ID列表
     * @return 分类ID到分类对象的映射
     */
    private Map<Long, Category> batchQueryCategories(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Category::getId, categoryIds);
        queryWrapper.eq(Category::getIsDel, 0); // 未删除的记录

        List<Category> categories = categoryMapper.selectList(queryWrapper);return categories.stream()
                .collect(Collectors.toMap(Category::getId, category -> category, (v1, v2) -> v1));
    }

    /**
     * 批量查询证书信息
     *
     * @param certificateIds 证书ID列表
     * @return 证书ID到证书对象的映射
     */
    private Map<Long, Certificate> batchQueryCertificates(List<Long> certificateIds) {
        if (certificateIds == null || certificateIds.isEmpty()) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<Certificate> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Certificate::getId, certificateIds);
        queryWrapper.eq(Certificate::getIsDel, 0); // 未删除的记录

        List<Certificate> certificates = certificateMapper.selectList(queryWrapper);
        return certificates.stream()
                .collect(Collectors.toMap(Certificate::getId, certificate -> certificate, (v1, v2) -> v1));
    }

    /**
     * 批量查询阶段数量
     *
     * @param mapIds 学习地图ID列表
     * @return 学习地图ID到阶段数量的映射
     */
    private Map<Long, Integer> batchQueryStageCount(List<Long> mapIds) {
        if (mapIds == null || mapIds.isEmpty()) {
            return new HashMap<>();
        }

        Map<Long, Integer> stageCountMap = new HashMap<>();

        LambdaQueryWrapper<LearningMapStage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(LearningMapStage::getMapId, mapIds);
        queryWrapper.eq(LearningMapStage::getIsDel, 0);
        // 未删除的记录
        List<LearningMapStage> stages = learningMapStageMapper.selectList(queryWrapper);
        
        // 统计每个学习地图的阶段数量
        for (LearningMapStage stage : stages) {
            Long mapId = stage.getMapId();
            stageCountMap.put(mapId, stageCountMap.getOrDefault(mapId, 0) + 1);
        }
        
        return stageCountMap;
    }
    
    /**
     * 批量查询学习人数和完成人数
     *
     * @param mapIds 学习地图ID列表
     * @param learnerCountMap 学习地图ID到学习人数的映射
     * @param completionCountMap 学习地图ID到完成人数的映射
     */
    private void batchQueryLearnerAndCompletionCount(List<Long> mapIds, Map<Long, Integer> learnerCountMap, Map<Long, Integer> completionCountMap) {
        if (mapIds == null || mapIds.isEmpty()) {
            return;
        }
        
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(UserLearningTask::getBizId, mapIds);
        queryWrapper.eq(UserLearningTask::getBizType, BizType.LEARNING_MAP); // 学习地图类型
        queryWrapper.eq(UserLearningTask::getIsDel, 0); // 未删除的记录
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        // 统计每个学习地图的学习人数和完成人数
        for (UserLearningTask task : learningTasks) {
            Long mapId = task.getBizId();
            // 统计学习人数
            learnerCountMap.put(mapId, learnerCountMap.getOrDefault(mapId, 0) + 1);
            // 统计完成人数
            if (task.getStatus() != null && task.getStatus().equals(LearningStatus.COMPLETED)) { // 状态为2表示已完成
                completionCountMap.put(mapId, completionCountMap.getOrDefault(mapId, 0) + 1);
            }
        }
    }
    
    /**
     * 将学习地图实体转换为DTO
     *
     * @param map 学习地图实体
     * @param categoryMap 分类ID到分类对象的映射
     * @param certificateMap 证书ID到证书对象的映射
     * @param stageCountMap 学习地图ID到阶段数量的映射
     * @param learnerCountMap 学习地图ID到学习人数的映射
     * @param completionCountMap 学习地图ID到完成人数的映射
     * @return 学习地图DTO
     */
    private LearningMapDTO convertToDTO(LearningMap map, Map<Long, Category> categoryMap, 
                                       Map<Long, Certificate> certificateMap,       Map<Long, Integer> stageCountMap, 
                                       Map<Long, Integer> learnerCountMap, 
                                       Map<Long, Integer> completionCountMap) {
        LearningMapDTO dto = new LearningMapDTO();
        dto.setId(map.getId());
        dto.setName(map.getName());
        dto.setCover(map.getCover());
        dto.setRequiredCredit(map.getRequiredCredit());
        dto.setElectiveCredit(map.getElectiveCredit());
        dto.setCertificateRule(map.getCertificateRule());
        dto.setCertificateId(map.getCertificateId());
        // 设置证书名称
        if (map.getCertificateId() != null && certificateMap.containsKey(map.getCertificateId())) {
            dto.setCertificateName(certificateMap.get(map.getCertificateId()).getName());
        }
        
        // 设置开放时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (map.getStartTime() != null) {
            dto.setStartTime(sdf.format(map.getStartTime()));
        }
        if (map.getEndTime() != null) {
            dto.setEndTime(sdf.format(map.getEndTime()));
        }
        
        dto.setUnlockMode(map.getUnlockMode());
        dto.setTheme(map.getTheme());
        dto.setCreatorName(map.getCreatorName());
        
        // 设置创建时间
        if (map.getGmtCreate() != null) {
            dto.setGmtCreate(sdf.format(map.getGmtCreate()));
        }
        
        // 设置分类名称列表
        List<String> categoryNames = new ArrayList<>();
        dto.setCategoryIds(map.getCategoryIds());
        if (StringUtils.isNotBlank(map.getCategoryIds())) {
            String[] categoryIdArray = map.getCategoryIds().split(",");
            for (String categoryIdStr : categoryIdArray) {
                try {
                    Long categoryId = Long.parseLong(categoryIdStr.trim());
                    if (categoryMap.containsKey(categoryId)) {
                        categoryNames.add(categoryMap.get(categoryId).getName());
                    }
                } catch (NumberFormatException e) {
                    log.warn("解析分类ID失败: {}", categoryIdStr);
                }
            }
        }
        dto.setCategoryNames(categoryNames);
        
        // 设置阶段数量
        dto.setStageCount(stageCountMap.getOrDefault(map.getId(), 0));
        
        // 设置学习人数和完成人数
        dto.setLearnerCount(learnerCountMap.getOrDefault(map.getId(), 0));
        dto.setCompletionCount(completionCountMap.getOrDefault(map.getId(), 0));
        dto.setEnableAutoAssign(map.getAttributes(LearningMap.ATTRIBUTES_KEY_ENABLE_AUTO_ASSIGN));
        return dto;
    }
}

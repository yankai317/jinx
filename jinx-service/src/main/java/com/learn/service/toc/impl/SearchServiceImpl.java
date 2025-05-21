package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizType;
import com.learn.dto.toc.search.SearchContentRequest;
import com.learn.dto.toc.search.SearchContentResponse;
import com.learn.dto.toc.search.SearchItemDTO;
import com.learn.infrastructure.repository.entity.Category;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.LearningMap;
import com.learn.infrastructure.repository.entity.Train;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.CategoryMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.LearningMapMapper;
import com.learn.infrastructure.repository.mapper.TrainMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.toc.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 */
@Service
@Slf4j
public class SearchServiceImpl implements SearchService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private TrainMapper trainMapper;

    @Autowired
    private LearningMapMapper learningMapMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private CommonRangeInterface commonRangeInterface;

    @Override
    public SearchContentResponse searchContent(Long userId, SearchContentRequest request) {
        log.info("搜索内容，userId:{}, request:{}", userId, request);

        SearchContentResponse response = new SearchContentResponse();
        List<SearchItemDTO> resultList = new ArrayList<>();

        // 获取用户可见的内容ID列表
        Map<String, List<Long>> visibleContentIds = getVisibleContentIds(userId);

        // 根据请求类型搜索不同内容
        if ("all".equals(request.getType()) || BizType.COURSE.equals(request.getType())) {
            List<SearchItemDTO> courseItems = searchCourses(userId, request.getKeyword(), visibleContentIds.get(BizType.COURSE));
            resultList.addAll(courseItems);
        }

        if ("all".equals(request.getType()) || BizType.TRAIN.equals(request.getType())) {
            List<SearchItemDTO> trainItems = searchTrains(userId, request.getKeyword(), visibleContentIds.get(BizType.TRAIN));
            resultList.addAll(trainItems);
        }

        if ("all".equals(request.getType()) || BizType.LEARNING_MAP.equals(request.getType())) {
            List<SearchItemDTO> mapItems = searchLearningMaps(userId, request.getKeyword(), visibleContentIds.get(BizType.LEARNING_MAP));
            resultList.addAll(mapItems);
        }

        // 分页处理
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        int total = resultList.size();
        int totalPages = (total + pageSize - 1) / pageSize;

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<SearchItemDTO> pagedList = fromIndex < total
                ? resultList.subList(fromIndex, toIndex)
                : new ArrayList<>();

        // 设置响应数据
        response.setList(pagedList);
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        response.setTotalPages(totalPages);

        return response;
    }

    /**
     * 获取用户可见的内容ID列表
     *
     * @param userId 用户ID
     * @return 可见内容ID列表
     */
    private Map<String, List<Long>> getVisibleContentIds(Long userId) {
        Map<String, List<Long>> result = new HashMap<>();
        // 查询用户可见的课程
        CommonRangeQueryRequest commonRequest = new CommonRangeQueryRequest();
        commonRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        commonRequest.setUserId(userId.toString());
        List<CommonRangeQueryResponse> commonResponses = commonRangeInterface.listCommonRangeByCondition(commonRequest);

        List<Long> courseIds = commonResponses.stream()
                .filter(response -> response.getType().equals(BizType.COURSE))
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        result.put(BizType.COURSE, courseIds);
        
        List<Long> trainIds = commonResponses.stream()
                .filter(response -> response.getType().equals(BizType.TRAIN))
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        result.put(BizType.TRAIN, trainIds);

        List<Long> mapIds = commonResponses.stream()
                .filter(response -> response.getType().equals(BizType.LEARNING_MAP))
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        result.put(BizType.LEARNING_MAP, mapIds);
        
        return result;
    }

    /**
     * 搜索课程
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param visibleCourseIds 可见课程ID列表
     * @return 搜索结果列表
     */
    private List<SearchItemDTO> searchCourses(Long userId, String keyword, List<Long> visibleCourseIds) {
        if (CollectionUtils.isEmpty(visibleCourseIds)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Courses::getIsDel, 0)
                .eq(Courses::getStatus, "published")
                .in(Courses::getId, visibleCourseIds);
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Courses::getTitle, keyword)
                    .or()
                    .like(Courses::getDescription, keyword));
        }
        
        List<Courses> courses = coursesMapper.selectList(queryWrapper);
        
        // 获取用户学习进度
        Map<Long, Integer> courseProgressMap = getCourseProgressMap(userId, courses.stream()
                .map(Courses::getId)
                .collect(Collectors.toList()));
        
        // 获取分类名称映射
        Map<Long, String> categoryNameMap = getCategoryNameMap();
        
        return courses.stream().map(course -> {
            SearchItemDTO dto = new SearchItemDTO();
            dto.setId(course.getId());
            dto.setTitle(course.getTitle());
            dto.setType(BizType.COURSE);
            dto.setContentType(course.getType());
            dto.setCoverImage(course.getCoverImage());
            dto.setCredit(course.getCredit());
            dto.setDescription(course.getDescription());
            dto.setProgress(courseProgressMap.getOrDefault(course.getId(), 0));
            dto.setCategoryIds(course.getCategoryIds());
            
            // 设置分类名称
            if (StringUtils.hasText(course.getCategoryIds())) {
                StringBuilder categoryNames = new StringBuilder();
                String[] categoryIds = course.getCategoryIds().split(",");
                for (int i = 0; i < categoryIds.length; i++) {
                    Long categoryId = Long.parseLong(categoryIds[i].trim());
                    String categoryName = categoryNameMap.get(categoryId);
                    if (categoryName != null) {
                        if (i > 0) {
                            categoryNames.append(",");
                        }
                        categoryNames.append(categoryName);
                    }
                }
                dto.setCategoryNames(categoryNames.toString());
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 搜索培训
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param visibleTrainIds 可见培训ID列表
     * @return 搜索结果列表
     */
    private List<SearchItemDTO> searchTrains(Long userId, String keyword, List<Long> visibleTrainIds) {
        if (CollectionUtils.isEmpty(visibleTrainIds)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Train> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Train::getIsDel, 0)
                .eq(Train::getStatus, "published")
                .in(Train::getId, visibleTrainIds);
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Train::getName, keyword)
                    .or()
                    .like(Train::getIntroduction, keyword));
        }
        
        List<Train> trains = trainMapper.selectList(queryWrapper);
        
        // 获取用户学习进度
        Map<Long, Integer> trainProgressMap = getTrainProgressMap(userId, trains.stream()
                .map(Train::getId)
                .collect(Collectors.toList()));
        
        // 获取分类名称映射
        Map<Long, String> categoryNameMap = getCategoryNameMap();
        
        return trains.stream().map(train -> {
            SearchItemDTO dto = new SearchItemDTO();
            dto.setId(train.getId());
            dto.setTitle(train.getName());
            dto.setType(BizType.TRAIN);
            dto.setContentType(""); // 培训没有具体内容类型
            dto.setCoverImage(train.getCover());
            dto.setCredit(train.getCredit());
            dto.setDescription(train.getIntroduction());
            dto.setProgress(trainProgressMap.getOrDefault(train.getId(), 0));
            dto.setCategoryIds(train.getCategoryIds());
            
            // 设置分类名称
            if (StringUtils.hasText(train.getCategoryIds())) {
                StringBuilder categoryNames = new StringBuilder();
                String[] categoryIds = train.getCategoryIds().split(",");
                for (int i = 0; i < categoryIds.length; i++) {
                    Long categoryId = Long.parseLong(categoryIds[i].trim());
                    String categoryName = categoryNameMap.get(categoryId);
                    if (categoryName != null) {
                        if (i > 0) {
                            categoryNames.append(",");
                        }
                        categoryNames.append(categoryName);
                    }
                }
                dto.setCategoryNames(categoryNames.toString());
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 搜索学习地图
     *
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @param visibleMapIds 可见学习地图ID列表
     * @return 搜索结果列表
     */
    private List<SearchItemDTO> searchLearningMaps(Long userId, String keyword, List<Long> visibleMapIds) {
        if (CollectionUtils.isEmpty(visibleMapIds)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<LearningMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningMap::getIsDel, 0)
                .in(LearningMap::getId, visibleMapIds);
        
        // 关键词搜索
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(LearningMap::getName, keyword)
                    .or()
                    .like(LearningMap::getIntroduction, keyword));
        }
        
        List<LearningMap> maps = learningMapMapper.selectList(queryWrapper);
        
        // 获取用户学习进度
        Map<Long, Integer> mapProgressMap = getMapProgressMap(userId, maps.stream()
                .map(LearningMap::getId)
                .collect(Collectors.toList()));
        
        // 获取分类名称映射
        Map<Long, String> categoryNameMap = getCategoryNameMap();
        
        return maps.stream().map(map -> {
            SearchItemDTO dto = new SearchItemDTO();
            dto.setId(map.getId());
            dto.setTitle(map.getName());
            dto.setType(BizType.LEARNING_MAP);
            dto.setContentType(""); // 学习地图没有具体内容类型
            dto.setCoverImage(map.getCover());
            dto.setCredit(map.getRequiredCredit().intValue()); // 使用必修学分
            dto.setDescription(map.getIntroduction());
            dto.setProgress(mapProgressMap.getOrDefault(map.getId(), 0));
            dto.setCategoryIds(map.getCategoryIds());
            
            // 设置分类名称
            if (StringUtils.hasText(map.getCategoryIds())) {
                StringBuilder categoryNames = new StringBuilder();
                String[] categoryIds = map.getCategoryIds().split(",");
                for (int i = 0; i < categoryIds.length; i++) {
                    Long categoryId = Long.parseLong(categoryIds[i].trim());
                    String categoryName = categoryNameMap.get(categoryId);
                    if (categoryName != null) {
                        if (i > 0) {
                            categoryNames.append(",");
                        }
                        categoryNames.append(categoryName);
                    }
                }
                dto.setCategoryNames(categoryNames.toString());
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取课程学习进度
     *
     * @param userId 用户ID
     * @param courseIds 课程ID列表
     * @return 课程进度映射
     */
    private Map<Long, Integer> getCourseProgressMap(Long userId, List<Long> courseIds) {
        if (CollectionUtils.isEmpty(courseIds)) {
            return new HashMap<>();
        }
        
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0)
                .eq(UserLearningTask::getBizType, BizType.COURSE)
                .in(UserLearningTask::getBizId, courseIds);
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        Map<Long, Integer> progressMap = new HashMap<>();
        for (UserLearningTask task : learningTasks) {
            progressMap.put(task.getBizId(), task.getProgress());
        }
        
        return progressMap;
    }

    /**
     * 获取培训学习进度
     *
     * @param userId 用户ID
     * @param trainIds 培训ID列表
     * @return 培训进度映射
     */
    private Map<Long, Integer> getTrainProgressMap(Long userId, List<Long> trainIds) {
        if (CollectionUtils.isEmpty(trainIds)) {
            return new HashMap<>();
        }
        
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0)
                .eq(UserLearningTask::getBizType, BizType.TRAIN)
                .in(UserLearningTask::getBizId, trainIds);
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        Map<Long, Integer> progressMap = new HashMap<>();
        for (UserLearningTask task : learningTasks) {
            progressMap.put(task.getBizId(), task.getProgress());
        }
        
        return progressMap;
    }

    /**
     * 获取学习地图学习进度
     *
     * @param userId 用户ID
     * @param mapIds 学习地图ID列表
     * @return 学习地图进度映射
     */
    private Map<Long, Integer> getMapProgressMap(Long userId, List<Long> mapIds) {
        if (CollectionUtils.isEmpty(mapIds)) {
            return new HashMap<>();
        }
        
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0)
                .eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                .in(UserLearningTask::getBizId, mapIds);
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        Map<Long, Integer> progressMap = new HashMap<>();
        for (UserLearningTask task : learningTasks) {
            progressMap.put(task.getBizId(), task.getProgress());
        }
        
        return progressMap;
    }

    /**
     * 获取分类名称映射
     *
     * @return 分类ID到名称的映射
     */
    private Map<Long, String> getCategoryNameMap() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getIsDel, 0);
        
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        
        return categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));
    }
}

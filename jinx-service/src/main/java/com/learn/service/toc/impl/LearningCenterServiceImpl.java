package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.constants.BizType;
import com.learn.dto.toc.learning.CategoryItemDTO;
import com.learn.dto.toc.learning.LearningCenterItemDTO;
import com.learn.dto.toc.learning.LearningCenterRequest;
import com.learn.dto.toc.learning.LearningCenterResponse;
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
import com.learn.service.toc.LearningCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学习中心服务实现类
 */
@Service
@Slf4j
public class LearningCenterServiceImpl implements LearningCenterService {

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
    public LearningCenterResponse getLearningCenter(Long userId, LearningCenterRequest request) {
        log.info("获取学习中心数据，userId:{}, request:{}", userId, request);

        LearningCenterResponse response = new LearningCenterResponse();

        // 1. 根据类型和分类获取内容
        List<LearningCenterItemDTO> contentList = new ArrayList<>();

        // 获取用户可见的内容ID列表
        Map<String, List<Long>> visibleContentIds = getVisibleContentIds(userId);

        // 根据请求类型获取不同内容
        if (StringUtils.isEmpty(request.getType()) || BizType.COURSE.equals(request.getType())) {
            List<LearningCenterItemDTO> courseItems = getCourseItems(userId, request, visibleContentIds.get(BizType.COURSE));
            contentList.addAll(courseItems);
        }

        if (StringUtils.isEmpty(request.getType()) || BizType.TRAIN.equals(request.getType())) {
            List<LearningCenterItemDTO> trainItems = getTrainItems(userId, request, visibleContentIds.get(BizType.TRAIN));
            contentList.addAll(trainItems);
        }

        if (StringUtils.isEmpty(request.getType()) || BizType.LEARNING_MAP.equals(request.getType())) {
            List<LearningCenterItemDTO> mapItems = getLearningMapItems(userId, request, visibleContentIds.get(BizType.LEARNING_MAP));
            contentList.addAll(mapItems);
        }
        
        if (StringUtils.isEmpty(request.getType()) || "exam".equals(request.getType())) {
            // 暂时返回空列表，后续实现考试内容查询
            List<LearningCenterItemDTO> examItems = new ArrayList<>();
            contentList.addAll(examItems);
        }
        
        if (StringUtils.isEmpty(request.getType()) || "practice".equals(request.getType())) {
            // 暂时返回空列表，后续实现练习内容查询
            List<LearningCenterItemDTO> practiceItems = new ArrayList<>();
            contentList.addAll(practiceItems);
        }

        // 2. 根据筛选条件过滤
        List<LearningCenterItemDTO> filteredList = filterContent(contentList, request);

        // 3. 分页处理
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        int total = filteredList.size();
        int totalPages = (total + pageSize - 1) / pageSize;

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<LearningCenterItemDTO> pagedList = fromIndex < total
                ? filteredList.subList(fromIndex, toIndex)
                : new ArrayList<>();

        // 5. 设置响应数据
        response.setList(pagedList);
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        response.setTotalPages(totalPages);

        return response;
    }

    /**
     * 获取分类列表
     *
     * @param categoryId 分类ID
     * @return 分类列表
     */
    private List<CategoryItemDTO> getCategoryList(Integer categoryId) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getIsDel, 0);
        
        // 如果指定了分类ID，则获取该分类及其子分类
        if (categoryId != null && categoryId > 0) {
            Category category = categoryMapper.selectById(categoryId);
            if (category != null) {
                queryWrapper.likeRight(Category::getPath, category.getPath());
            }
        }
        
        List<Category> categories = categoryMapper.selectList(queryWrapper);
        
        // 统计每个分类下的内容数量
        Map<Long, Integer> categoryCounts = getCategoryCounts();
        
        return categories.stream().map(category -> {
            CategoryItemDTO dto = new CategoryItemDTO();
            dto.setId(category.getId());
            dto.setName(category.getName());
            dto.setCount(categoryCounts.getOrDefault(category.getId(), 0));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 统计每个分类下的内容数量
     *
     * @return 分类内容数量映射
     */
    private Map<Long, Integer> getCategoryCounts() {
        Map<Long, Integer> categoryCounts = new HashMap<>();
        
        // 统计课程分类数量
        LambdaQueryWrapper<Courses> courseWrapper = new LambdaQueryWrapper<>();
        courseWrapper.eq(Courses::getIsDel, 0)
                .eq(Courses::getStatus, "published");
        List<Courses> courses = coursesMapper.selectList(courseWrapper);
        
        for (Courses course : courses) {
            if (StringUtils.hasText(course.getCategoryIds())) {
                String[] categoryIds = course.getCategoryIds().split(",");
                for (String categoryId : categoryIds) {
                    Long cid = Long.parseLong(categoryId.trim());
                    categoryCounts.put(cid, categoryCounts.getOrDefault(cid, 0) + 1);
                }
            }
        }
        
        // 统计培训分类数量
        LambdaQueryWrapper<Train> trainWrapper = new LambdaQueryWrapper<>();
        trainWrapper.eq(Train::getIsDel, 0)
                .eq(Train::getStatus, "published");
        List<Train> trains = trainMapper.selectList(trainWrapper);
        
        for (Train train : trains) {
            if (StringUtils.hasText(train.getCategoryIds())) {
                String[] categoryIds = train.getCategoryIds().split(",");
                for (String categoryId : categoryIds) {
                    Long cid = Long.parseLong(categoryId.trim());
                    categoryCounts.put(cid, categoryCounts.getOrDefault(cid, 0) + 1);
                }
            }
        }
        
        // 统计学习地图分类数量
        LambdaQueryWrapper<LearningMap> mapWrapper = new LambdaQueryWrapper<>();
        mapWrapper.eq(LearningMap::getIsDel, 0);
        List<LearningMap> maps = learningMapMapper.selectList(mapWrapper);
        
        for (LearningMap map : maps) {
            if (StringUtils.hasText(map.getCategoryIds())) {
                String[] categoryIds = map.getCategoryIds().split(",");
                for (String categoryId : categoryIds) {
                    Long cid = Long.parseLong(categoryId.trim());
                    categoryCounts.put(cid, categoryCounts.getOrDefault(cid, 0) + 1);
                }
            }
        }
        return categoryCounts;
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
        CommonRangeQueryRequest courseRequest = new CommonRangeQueryRequest();
        courseRequest.setModelType(RangeModelTypeEnums.VISIBILITY.getCode());
        courseRequest.setUserId(userId.toString());
        
        List<CommonRangeQueryResponse> responses = commonRangeInterface.listCommonRangeByCondition(courseRequest);
        if (CollectionUtils.isEmpty(responses)) {
            return new HashMap<>();
        }
        // 课程
        List<Long> courseIds = responses.stream()
                .filter(response -> BizType.COURSE.equals(response.getType()))
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        result.put(BizType.COURSE, courseIds);
        // 培训
        List<Long> trainIds = responses.stream()
                .filter(response -> BizType.TRAIN.equals(response.getType()))
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        result.put(BizType.TRAIN, trainIds);
        // 地图
        List<Long> mapIds = responses.stream()
                .filter(response -> BizType.LEARNING_MAP.equals(response.getType()))
                .map(CommonRangeQueryResponse::getTypeId)
                .collect(Collectors.toList());
        result.put(BizType.LEARNING_MAP, mapIds);
        
        return result;
    }

    /**
     * 获取课程列表
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @param visibleCourseIds 可见课程ID列表
     * @return 课程列表
     */
    private List<LearningCenterItemDTO> getCourseItems(Long userId, LearningCenterRequest request, List<Long> visibleCourseIds) {
        if (CollectionUtils.isEmpty(visibleCourseIds)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Courses::getIsDel, 0)
                .eq(Courses::getStatus, "published")
                .in(Courses::getId, visibleCourseIds);
        
        // 分类筛选
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(request.getCategoryIds())) {
            queryWrapper.in(Courses::getCategoryIds, request.getCategoryIds());
        }
        
        // 内容类型筛选
        if (StringUtils.hasText(request.getContentTypes())) {
            String[] contentTypes = request.getContentTypes().split(",");
            queryWrapper.in(Courses::getType, (Object[]) contentTypes);
        }
        
        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Courses::getTitle, request.getKeyword())
                    .or()
                    .like(Courses::getDescription, request.getKeyword()));
        }
        
        List<Courses> courses = coursesMapper.selectList(queryWrapper);
        
        // 获取用户学习进度
        Map<Long, Integer> courseProgressMap = getCourseProgressMap(userId, courses.stream()
                .map(Courses::getId)
                .collect(Collectors.toList()));
        
        return courses.stream().map(course -> {
            LearningCenterItemDTO dto = new LearningCenterItemDTO();
            dto.setId(course.getId());
            dto.setTitle(course.getTitle());
            dto.setType(BizType.COURSE);
            dto.setContentType(course.getType());
            dto.setCoverImage(course.getCoverImage());
            dto.setCredit(course.getCredit());
            dto.setIsRequired(0); // 默认非必修
            dto.setProgress(courseProgressMap.getOrDefault(course.getId(), 0));
            dto.setIsTop(course.getIsTop());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取培训列表
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @param visibleTrainIds 可见培训ID列表
     * @return 培训列表
     */
    private List<LearningCenterItemDTO> getTrainItems(Long userId, LearningCenterRequest request, List<Long> visibleTrainIds) {
        if (CollectionUtils.isEmpty(visibleTrainIds)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<Train> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Train::getIsDel, 0)
                .eq(Train::getStatus, "published")
                .in(Train::getId, visibleTrainIds);
        
        // 分类筛选
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(request.getCategoryIds())) {
            queryWrapper.in(Train::getCategoryIds, request.getCategoryIds());
        }
        
        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Train::getName, request.getKeyword())
                    .or()
                    .like(Train::getIntroduction, request.getKeyword()));
        }
        
        List<Train> trains = trainMapper.selectList(queryWrapper);
        
        // 获取用户学习进度
        Map<Long, Integer> trainProgressMap = getTrainProgressMap(userId, trains.stream()
                .map(Train::getId)
                .collect(Collectors.toList()));
        
        return trains.stream().map(train -> {
            LearningCenterItemDTO dto = new LearningCenterItemDTO();
            dto.setId(train.getId());
            dto.setTitle(train.getName());
            dto.setType(BizType.TRAIN);
            dto.setContentType(""); // 培训没有具体内容类型
            dto.setCoverImage(train.getCover());
            dto.setCredit(train.getCredit());
            dto.setIsRequired(0); // 默认非必修
            dto.setProgress(trainProgressMap.getOrDefault(train.getId(), 0));
            dto.setIsTop(0); // 培训没有置顶字段
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 获取学习地图列表
     *
     * @param userId 用户ID
     * @param request 请求参数
     * @param visibleMapIds 可见学习地图ID列表
     * @return 学习地图列表
     */
    private List<LearningCenterItemDTO> getLearningMapItems(Long userId, LearningCenterRequest request, List<Long> visibleMapIds) {
        if (CollectionUtils.isEmpty(visibleMapIds)) {
            return new ArrayList<>();
        }
        
        LambdaQueryWrapper<LearningMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LearningMap::getIsDel, 0)
                .in(LearningMap::getId, visibleMapIds);
        
        // 分类筛选
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(request.getCategoryIds())) {
            queryWrapper.in(LearningMap::getCategoryIds, request.getCategoryIds());
        }
        
        // 关键词搜索
        if (StringUtils.hasText(request.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(LearningMap::getName, request.getKeyword())
                    .or()
                    .like(LearningMap::getIntroduction, request.getKeyword()));
        }
        
        List<LearningMap> maps = learningMapMapper.selectList(queryWrapper);
        
        // 获取用户学习进度
        Map<Long, Integer> mapProgressMap = getMapProgressMap(userId, maps.stream()
                .map(LearningMap::getId)
                .collect(Collectors.toList()));
        
        return maps.stream().map(map -> {
            LearningCenterItemDTO dto = new LearningCenterItemDTO();
            dto.setId(map.getId());
            dto.setTitle(map.getName());
            dto.setType(BizType.LEARNING_MAP);
            dto.setContentType(""); // 学习地图没有具体内容类型
            dto.setCoverImage(map.getCover());
            dto.setCredit(map.getRequiredCredit().intValue()); // 使用必修学分
            dto.setIsRequired(0); // 默认非必修
            dto.setProgress(mapProgressMap.getOrDefault(map.getId(), 0));
            dto.setIsTop(0); // 学习地图没有置顶字段
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
                .eq(UserLearningTask::getBizType, BizType.COURSE)
                .in(UserLearningTask::getBizId, courseIds)
                .eq(UserLearningTask::getIsDel, 0);
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        Map<Long, Integer> progressMap = new HashMap<>();
        for (UserLearningTask task : learningTasks) {
            // 直接使用进度字段
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
                .eq(UserLearningTask::getBizType, BizType.TRAIN)
                .in(UserLearningTask::getBizId, trainIds)
                .eq(UserLearningTask::getIsDel, 0);
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        Map<Long, Integer> progressMap = new HashMap<>();
        for (UserLearningTask task : learningTasks) {
            // 直接使用进度字段
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
                .eq(UserLearningTask::getBizType, BizType.LEARNING_MAP)
                .in(UserLearningTask::getBizId, mapIds)
                .eq(UserLearningTask::getIsDel, 0);
        
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        Map<Long, Integer> progressMap = new HashMap<>();
        for (UserLearningTask task : learningTasks) {
            // 直接使用进度字段
            progressMap.put(task.getBizId(), task.getProgress());
        }
        
        return progressMap;
    }

    /**
     * 根据筛选条件过滤内容
     *
     * @param contentList 内容列表
     * @param request 请求参数
     * @return 过滤后的内容列表
     */
    private List<LearningCenterItemDTO> filterContent(List<LearningCenterItemDTO> contentList, LearningCenterRequest request) {
        if (CollectionUtils.isEmpty(contentList)) {
            return new ArrayList<>();
        }
        
        // 内容类型筛选
        if (StringUtils.hasText(request.getContentTypes())) {
            String[] contentTypes = request.getContentTypes().split(",");
            List<String> contentTypeList = Arrays.asList(contentTypes);
            contentList = contentList.stream()
                    .filter(item -> contentTypeList.contains(item.getContentType()))
                    .collect(Collectors.toList());
        }
        
        // 按置顶和ID排序
        contentList.sort((a, b) -> {
            // 先按置顶排序
            int topCompare = b.getIsTop().compareTo(a.getIsTop());
            if (topCompare != 0) {
                return topCompare;
            }
            // 再按ID倒序排序（假设ID越大越新）
            return b.getId().compareTo(a.getId());
        });
        
        return contentList;
    }
}

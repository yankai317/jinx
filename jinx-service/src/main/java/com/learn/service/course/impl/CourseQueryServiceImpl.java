package com.learn.service.course.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.exception.CommonException;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.dto.course.CourseDetailDTO;
import com.learn.dto.course.CourseListDTO;
import com.learn.dto.course.CourseListRequest;
import com.learn.dto.course.CourseListResponse;
import com.learn.dto.course.CourseStatisticsDTO;
import com.learn.dto.course.sub.SeriesCourseFile;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.CommonRangeInterface;
import com.learn.service.category.CategoryQueryService;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.course.CourseQueryService;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程查询服务实现类
 */
@Service
@Slf4j
public class CourseQueryServiceImpl implements CourseQueryService {

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryQueryService categoryQueryService;
    
    @Autowired
    private CommonRangeInterface commonRangeInterface;
    
    @Autowired
    private RangeSetService rangeSetService;
    
    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private DepartmentUserMapper departmentUserMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Override
    public CourseStatisticsDTO getCourseStatistics(Long id) {
        log.info("获取课程统计数据，课程ID：{}", id);
        
        // 参数校验
        if (id == null) {
            throw new CommonException("课程ID不能为空");
        }
        
        // 查询课程信息
        Courses course = coursesMapper.selectById(id);
        if (course == null || course.getIsDel() == 1) {
            throw new CommonException("课程不存在");
        }
        
        // 构建统计数据DTO
        CourseStatisticsDTO statisticsDTO = new CourseStatisticsDTO();
        
        // 设置查看数和完成人数
        statisticsDTO.setViewCount(course.getViewCount());
        statisticsDTO.setCompleteCount(course.getCompleteCount());
        
        // 计算学习时长相关数据
        calculateDurationStatistics(id, statisticsDTO);
        
        // 统计部门学习情况
        calculateDepartmentStatistics(id, statisticsDTO);// 统计时间分布情况
        calculateTimeDistribution(id, statisticsDTO);
        
        return statisticsDTO;
    }
    /**
     * 计算学习时长相关统计数据
     *
     * @param courseId 课程ID
     * @param statisticsDTO 统计数据DTO
     */
    private void calculateDurationStatistics(Long courseId, CourseStatisticsDTO statisticsDTO) {
        // 查询该课程的所有学习完成记录
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, "COURSE");
        queryWrapper.eq(UserLearningTask::getBizId, courseId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);
        // 只统计已完成的记录
        queryWrapper.eq(UserLearningTask::getStatus, 2);
        List<UserLearningTask> completionList = userLearningTaskMapper.selectList(queryWrapper);
        
        if (completionList.isEmpty()) {
            statisticsDTO.setAvgDuration(0);
            statisticsDTO.setTotalDuration(0);
            return;
        }
        
        // 计算总学习时长
        int totalDuration = completionList.stream()
                .mapToInt(UserLearningTask::getStudyDuration)
                .sum();
        // 计算平均学习时长
        int avgDuration = totalDuration / completionList.size();
        
        statisticsDTO.setTotalDuration(totalDuration);
        statisticsDTO.setAvgDuration(avgDuration);
    }
    
    /**
     * 统计部门学习情况
     *
     * @param courseId 课程ID
     * @param statisticsDTO 统计数据DTO
     */
    private void calculateDepartmentStatistics(Long courseId, CourseStatisticsDTO statisticsDTO) {
        // 查询该课程的所有学习完成记录
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, "COURSE");
        queryWrapper.eq(UserLearningTask::getBizId, courseId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);
        // 只统计已完成的记录
        queryWrapper.eq(UserLearningTask::getStatus, 2);
        List<UserLearningTask> completionList = userLearningTaskMapper.selectList(queryWrapper);
        
        if (completionList.isEmpty()) {
            statisticsDTO.setDepartmentStats(new ArrayList<>());
            return;
        }
        
        // 获取所有完成学习的用户ID
        List<Long> userIds = completionList.stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());
        
        // 查询这些用户所属的部门
        LambdaQueryWrapper<DepartmentUser> duQueryWrapper = new LambdaQueryWrapper<>();
        duQueryWrapper.in(DepartmentUser::getUserId, userIds);
        duQueryWrapper.eq(DepartmentUser::getIsDel, 0);
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duQueryWrapper);
        
        if (departmentUsers.isEmpty()) {
            statisticsDTO.setDepartmentStats(new ArrayList<>());
            return;
        }
        
        // 获取所有部门ID
        List<Long> departmentIds = departmentUsers.stream()
                .map(DepartmentUser::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());
        
        // 查询部门信息
        LambdaQueryWrapper<Department> deptQueryWrapper = new LambdaQueryWrapper<>();
        deptQueryWrapper.in(Department::getId, departmentIds);deptQueryWrapper.eq(Department::getIsDel, 0);
        List<Department> departments = departmentMapper.selectList(deptQueryWrapper);// 构建部门ID到部门名称的映射
        Map<Long, String> departmentIdToNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));// 统计每个部门的学习人数
        Map<Long, Long> departmentCountMap = departmentUsers.stream()
                .collect(Collectors.groupingBy(DepartmentUser::getDepartmentId, Collectors.counting()));
        
        // 构建部门统计数据
        List<CourseStatisticsDTO.DepartmentStatDTO> departmentStats = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : departmentCountMap.entrySet()) {
            Long departmentId = entry.getKey();
            Long count = entry.getValue();
            String departmentName = departmentIdToNameMap.getOrDefault(departmentId, "未知部门");
            
            CourseStatisticsDTO.DepartmentStatDTO departmentStat = CourseStatisticsDTO.DepartmentStatDTO.builder()
                    .name(departmentName)
                    .count(count.intValue())
                    .build();
            
            departmentStats.add(departmentStat);
        }
        
        // 按人数降序排序
        departmentStats.sort((a, b) -> b.getCount() - a.getCount());
        
        statisticsDTO.setDepartmentStats(departmentStats);
    }
    
    /**
     * 统计时间分布情况
     *
     * @param courseId 课程ID
     * @param statisticsDTO 统计数据DTO
     */
    private void calculateTimeDistribution(Long courseId, CourseStatisticsDTO statisticsDTO) {
        // 查询该课程的所有学习记录
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, "COURSE");
        queryWrapper.eq(UserLearningTask::getBizId, courseId);
        queryWrapper.eq(UserLearningTask::getIsDel, 0);
        // 查询所有有开始学习时间的记录
        queryWrapper.isNotNull(UserLearningTask::getStartTime);
        List<UserLearningTask> recordsList = userLearningTaskMapper.selectList(queryWrapper);
        
        if (recordsList.isEmpty()) {
            statisticsDTO.setTimeDistribution(new ArrayList<>());
            return;
        }
        
        // 按日期分组统计
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Integer> dateCountMap = new HashMap<>();
        
        for (UserLearningTask record : recordsList) {
            if (record.getStartTime() != null) {
                String dateStr = dateFormat.format(record.getStartTime());
                dateCountMap.put(dateStr, dateCountMap.getOrDefault(dateStr, 0) + 1);
            }
        }
        
        // 构建时间分布数据
        List<CourseStatisticsDTO.TimeDistributionDTO> timeDistribution = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dateCountMap.entrySet()) {
            CourseStatisticsDTO.TimeDistributionDTO distributionDTO = CourseStatisticsDTO.TimeDistributionDTO.builder()
                    .date(entry.getKey())
                    .count(entry.getValue())
                    .build();
            timeDistribution.add(distributionDTO);
        }
        
        // 按日期升序排序
        timeDistribution.sort(Comparator.comparing(CourseStatisticsDTO.TimeDistributionDTO::getDate));
        
        statisticsDTO.setTimeDistribution(timeDistribution);
    }

    @Override
    public CourseDetailDTO getCourseDetail(Long id) {
        log.info("获取课程详情，课程ID：{}", id);
        
        // 参数校验
        if (id == null) {
            throw new CommonException("课程ID不能为空");
        }
        
        // 查询课程信息
        Courses course = coursesMapper.selectById(id);
        if (course == null || course.getIsDel() == 1) {
            throw new CommonException("课程不存在");
        }
        
        // 构建课程详情DTO
        CourseDetailDTO courseDetailDTO = new CourseDetailDTO();
        courseDetailDTO.setId(course.getId());
        courseDetailDTO.setTitle(course.getTitle());
        courseDetailDTO.setType(course.getType());
        courseDetailDTO.setCoverImage(course.getCoverImage());
        courseDetailDTO.setInstructorId(course.getInstructorId());
        courseDetailDTO.setGuestName(course.getGuestName());
        courseDetailDTO.setDescription(course.getDescription());
        courseDetailDTO.setCredit(course.getCredit());
        courseDetailDTO.setStatus(course.getStatus());
        courseDetailDTO.setAllowComments(course.getAllowComments() == 1);
        courseDetailDTO.setIsTop(course.getIsTop() == 1);
        courseDetailDTO.setViewCount(course.getViewCount());
        courseDetailDTO.setCompleteCount(course.getCompleteCount());
        courseDetailDTO.setArticle(course.getArticle());
        courseDetailDTO.setAppendixType(course.getAppendixType());
        courseDetailDTO.setAppendixPath(course.getAppendixPath());
        courseDetailDTO.setCreatorId(course.getCreatorId());
        courseDetailDTO.setCreatorName(course.getCreatorName());
        courseDetailDTO.setIfIsCitable(course.getIfIsCitable());

        // 格式化发布时间
        if (course.getPublishTime() != null) {
            courseDetailDTO.setPublishTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(course.getPublishTime()));
        }

        // 设置附件类型
        if ("series".equals(course.getType())) {
            LambdaQueryWrapper<ContentRelation> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(ContentRelation::getBizId, course.getId())
                    .eq(ContentRelation::getBizType, BizType.COURSE)
                    .eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                    .eq(ContentRelation::getIsDel, 0);
            List<ContentRelation> relations = contentRelationMapper.selectList(queryWrapper);
            List<SeriesCourseFile> appendixFiles = relations.stream().map(v -> {
                SeriesCourseFile seriesCourseFile = new SeriesCourseFile();
                seriesCourseFile.setName(v.getAttribute(AttributeKey.TITLE));
                seriesCourseFile.setUrl(v.getContentUrl());
                seriesCourseFile.setBizType(v.getAttribute(AttributeKey.BIZ_TYPE));
                seriesCourseFile.setType(v.getAttribute(AttributeKey.TYPE));
                seriesCourseFile.setDuration(Objects.isNull(v.getAttribute(AttributeKey.DURATION)) ? null : Integer.parseInt(v.getAttribute(AttributeKey.DURATION)));
                return seriesCourseFile;
            }).collect(Collectors.toList());
            courseDetailDTO.setAppendixPath(Json.toJson(appendixFiles));
        }
        
        // 处理分类信息
        processCategoryInfo(course, courseDetailDTO);
        
        // 处理可见范围信息
        courseDetailDTO.setVisibility(rangeSetService.processVisibilityInfo(BizType.COURSE, course.getId()));

        // 处理协同管理信息
        courseDetailDTO.setCollaborators(rangeSetService.processCollaboratorsInfo(BizType.COURSE, course.getId()));

        return courseDetailDTO;
    }
    
    /**
     * 处理分类信息
     *
     * @param course 课程实体
     * @param courseDetailDTO 课程详情DTO
     */
    private void processCategoryInfo(Courses course, CourseDetailDTO courseDetailDTO) {
        List<String> categoryNames = new ArrayList<>();
        
        if (StringUtils.isNotBlank(course.getCategoryIds())) {
            Category category = categoryMapper.selectById(course.getCategoryIds());
            String[] ids = course.getCategoryIds().split(",");
//            courseDetailDTO.setCategoryIds(Arrays.stream(ids).map(Long::parseLong).collect(Collectors.toList()));
            courseDetailDTO.setCategoryIds(course.getCategoryIds());
            categoryNames.add(category.getName());
        }
        
        courseDetailDTO.setCategoryNames(categoryNames);
    }
    
    @Override
    public CourseListResponse getCourseList(CourseListRequest request) {
        log.info("获取课程列表，请求参数：{}", request);

        // 构建查询条件
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        // 设置未删除条件
        queryWrapper.eq(Courses::getIsDel, 0);
        
        // 课程名称模糊匹配
        if (StringUtils.isNotBlank(request.getTitle())) {
            queryWrapper.like(Courses::getTitle, request.getTitle());
        }
        
        // 课程类型
        if (StringUtils.isNotBlank(request.getType())) {
            queryWrapper.eq(Courses::getType, request.getType());
        }
        
        // 状态
        if (StringUtils.isNotBlank(request.getStatus())) {
            queryWrapper.eq(Courses::getStatus, request.getStatus());
        }

        if (Objects.nonNull(request.getIfIsCitable())) {
            queryWrapper.eq(Courses::getIfIsCitable, request.getIfIsCitable());
        }

        // 分类ID
        if (CollectionUtils.isNotEmpty(request.getCategoryIds())) {
            List<Long> categoryIds = categoryQueryService.fullSearchCategoryId(request.getCategoryIds());
            queryWrapper.in(Courses::getCategoryIds, categoryIds);
        }
        
        // 创建人ID
        if (request.getCreatorId() != null) {
            queryWrapper.eq(Courses::getCreatorId, request.getCreatorId());
        }
        
        // 只看我创建的
        if (request.getOnlyMine() != null && request.getOnlyMine() && request.getCreatorId() != null) {
            queryWrapper.eq(Courses::getCreatorId, request.getCreatorId());
        }
        
        // 创建时间范围
        if (StringUtils.isNotBlank(request.getStartTime())) {
            try {
                Date startDate;
                if (request.getStartTime().length() <= 10) {
                    // 处理 yyyy-MM-dd 格式
                    startDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getStartTime());
                } else {
                    // 处理 yyyy-MM-dd HH:mm:ss 格式
                    startDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.getStartTime());
                }
                queryWrapper.ge(Courses::getGmtCreate, startDate);
            } catch (ParseException e) {
                log.error("解析开始时间失败", e);
            }
        }
        
        if (StringUtils.isNotBlank(request.getEndTime())) {
            try {
                Date endDate;
                if (request.getEndTime().length() <= 10) {
                    // 处理 yyyy-MM-dd 格式
                    endDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getEndTime());
                } else {
                    // 处理 yyyy-MM-dd HH:mm:ss 格式
                    endDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(request.getEndTime());
                }
                queryWrapper.le(Courses::getGmtCreate, endDate);
            } catch (ParseException e) {
                log.error("解析结束时间失败", e);
            }
        }
        
        // 排序
        if (StringUtils.isNotBlank(request.getSortField())) {
            String sortField = request.getSortField();
            boolean isAsc = "asc".equalsIgnoreCase(request.getSortOrder());
            
            // 根据不同的排序字段设置排序
            switch (sortField) {
                case "gmt_create":
                    queryWrapper.orderBy(true, isAsc, Courses::getGmtCreate);
                    break;
                case "view_count":
                    queryWrapper.orderBy(true, isAsc, Courses::getViewCount);
                    break;
                case "complete_count":
                    queryWrapper.orderBy(true, isAsc, Courses::getCompleteCount);
                    break;
                default:
                    queryWrapper.orderByDesc(Courses::getGmtCreate);
                    break;
            }
        } else {
            // 默认按创建时间倒序
            queryWrapper.orderByDesc(Courses::getGmtCreate);
        }
        
        // 分页查询
        int pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        IPage<Courses> page = new Page<>(pageNum, pageSize);
        IPage<Courses> coursesPage = coursesMapper.selectPage(page, queryWrapper);
        
        // 获取课程列表
        List<Courses> coursesList = coursesPage.getRecords();
        
        // 如果课程列表不为空，批量查询查看数和完成数
        if (!coursesList.isEmpty()) {
            // 提取课程ID列表
            List<Long> courseIds = coursesList.stream()
                    .map(Courses::getId)
                    .collect(Collectors.toList());
            
            // 批量查询查看数和完成数
            List<Map<String, Object>> statsResults = coursesMapper.batchCountViewAndComplete(courseIds);
            
            // 构建课程ID到统计数据的映射
            Map<Long, Map<String, Object>> courseStatsMap = new HashMap<>();
            for (Map<String, Object> statsResult : statsResults) {
                Long courseId = ((Number) statsResult.get("courseId")).longValue();
                courseStatsMap.put(courseId, statsResult);
            }
            
            // 更新课程列表中的查看数和完成数
            for (Courses course : coursesList) {
                Map<String, Object> stats = courseStatsMap.get(course.getId());
                if (stats != null) {
                    // 设置查看数
                    if (stats.get("viewCount") != null) {
                        course.setViewCount(((Number) stats.get("viewCount")).intValue());
                    }
                    
                    // 设置完成数
                    if (stats.get("completeCount") != null) {
                        course.setCompleteCount(((Number) stats.get("completeCount")).intValue());
                    }
                }
            }
        }

        // 转换为DTO
        List<CourseListDTO> courseListDTOs = convertToCourseDTOList(coursesList);
        
        // 构建响应
        CourseListResponse response = CourseListResponse.builder()
                .total(coursesPage.getTotal())
                .list(courseListDTOs)
                .build();
                
        return response;
    }

    
    /**
     * 将课程实体列表转换为DTO列表
     *
     * @param coursesList 课程实体列表
     * @return 课程DTO列表
     */
    private List<CourseListDTO> convertToCourseDTOList(List<Courses> coursesList) {
        if (coursesList == null || coursesList.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 收集所有课程的分类ID
        Set<Long> categoryIds = new HashSet<>();
        for (Courses course : coursesList) {
            if (StringUtils.isNotBlank(course.getCategoryIds())) {
                String[] ids = course.getCategoryIds().split(",");
                for (String id : ids) {
                    try {
                        categoryIds.add(Long.parseLong(id));
                    } catch (NumberFormatException e) {
                        log.error("解析分类ID失败: {}", id, e);
                    }
                }
            }
        }// 批量查询分类信息
        Map<Long, Category> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<Category> categoryQueryWrapper = new LambdaQueryWrapper<>();
            categoryQueryWrapper.in(Category::getId, categoryIds);
            categoryQueryWrapper.eq(Category::getIsDel, 0);
            List<Category> categories = categoryMapper.selectList(categoryQueryWrapper);
            for (Category category : categories) {
                categoryMap.put(category.getId(), category);
            }
        }
        
        // 转换课程列表
        return coursesList.stream().map(course -> {
            CourseListDTO dto = new CourseListDTO();
            dto.setId(course.getId());
            dto.setTitle(course.getTitle());
            dto.setType(course.getType());
            dto.setCategoryIds(course.getCategoryIds());
            dto.setCoverImage(course.getCoverImage());
            dto.setCredit(course.getCredit());
            dto.setStatus(course.getStatus());
            dto.setViewCount(course.getViewCount());
            dto.setCompleteCount(course.getCompleteCount());
            dto.setCreatorName(course.getCreatorName());
            // 格式化创建时间
            if (course.getGmtCreate() != null) {
                dto.setGmtCreate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(course.getGmtCreate()));
            }
            
            // 设置分类名称列表
            List<String> categoryNames = new ArrayList<>();
            if (StringUtils.isNotBlank(course.getCategoryIds())) {
                String[] ids = course.getCategoryIds().split(",");
                for (String id : ids) {
                    try {
                        Long categoryId = Long.parseLong(id);
                        Category category = categoryMap.get(categoryId);
                        if (category != null) {
                            categoryNames.add(category.getName());
                        }
                    } catch (NumberFormatException e) {
                        log.error("解析分类ID失败: {}", id, e);
                    }
                }
            }
            dto.setCategoryNames(categoryNames);
            
            return dto;
        }).collect(Collectors.toList());
    }
}

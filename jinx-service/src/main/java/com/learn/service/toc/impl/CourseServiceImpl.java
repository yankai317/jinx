package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.AttributeKey;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.course.CourseDetailResponse;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.toc.video.VideoPlayRequest;
import com.learn.dto.toc.video.VideoPlayResponse;
import com.learn.dto.user.UserStudyRecordCreateDTO;
import com.learn.infrastructure.repository.entity.ContentRelation;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.ContentRelationMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.toc.CourseService;
import com.learn.util.JinxDateUtil;
import dev.ai4j.openai4j.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * C端课程服务实现类
 */
@Service
@Slf4j
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ContentRelationMapper contentRelationMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private com.learn.service.user.UserStudyService userStudyService;

    /**
     * 获取课程详情
     *
     * @param courseId 课程ID
     * @param userId   用户ID
     * @return 课程详情
     */
    @Override
    public CourseDetailResponse getCourseDetail(Long courseId, Long userId) {
        // 1. 获取课程基本信息
        Courses course = getCourseById(courseId);
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        // 2. 获取学习人员列表
        List<CourseDetailResponse.LearnerDTO> learners = getLearners(courseId);

        // 3. 获取课程章节列表
        List<CourseDetailResponse.CourseItemDTO> courseItems = getCourseItems(courseId, userId);

        // 4. 获取用户学习进度
        CourseDetailResponse.UserProgressDTO userProgress = getUserProgress(userId, courseId, course, courseItems);

        // 5. 构建课程详情响应
        return buildCourseDetailResponse(course, userProgress, learners, courseItems);
    }

    /**
     * 获取课程章节列表
     *
     * @param courseId 课程ID
     * @param userId   用户ID
     * @return 课程章节列表
     */
    private List<CourseDetailResponse.CourseItemDTO> getCourseItems(Long courseId, Long userId) {
        // 获取课程信息，判断是否为系列课
        Courses course = getCourseById(courseId);
        if (course == null || !"series".equals(course.getType())) {
            return Collections.emptyList();
        }

        // 1. 查询课程章节关联关系
        LambdaQueryWrapper<ContentRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ContentRelation::getBizId, courseId)
                .eq(ContentRelation::getBizType, BizType.COURSE)
                .eq(ContentRelation::getContentType, BizType.APPENDIX_FILE)
                .eq(ContentRelation::getIsDel, 0)
                .orderByAsc(ContentRelation::getSortOrder);
        List<ContentRelation> contentRelations = contentRelationMapper.selectList(queryWrapper);

        if (contentRelations == null || contentRelations.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询用户在该系列课下的所有章节学习进度
        Map<Long, UserLearningTask> learningTaskMap = new HashMap<>();
        if (userId != null) {
            LambdaQueryWrapper<UserLearningTask> taskQueryWrapper = new LambdaQueryWrapper<>();
            taskQueryWrapper.eq(UserLearningTask::getParentId, courseId)
                    .eq(UserLearningTask::getParentType, BizType.COURSE)
                    .eq(UserLearningTask::getUserId, userId)
                    .eq(UserLearningTask::getIsDel, 0);
            List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(taskQueryWrapper);
            
            // 将学习任务按bizId(对应courseItem的Id)进行映射，方便后续快速查找
            if (learningTasks != null && !learningTasks.isEmpty()) {
                for (UserLearningTask task : learningTasks) {
                    learningTaskMap.put(task.getBizId(), task);
                }
            }
        }

        // 3. 构建课程章节列表
        List<CourseDetailResponse.CourseItemDTO> courseItems = new ArrayList<>();
        for (ContentRelation relation : contentRelations) {
            CourseDetailResponse.CourseItemDTO courseItem = new CourseDetailResponse.CourseItemDTO();
            courseItem.setId(relation.getContentId());
            if (StringUtils.hasText(relation.getAttributes())) {
                Map<String, String> attributeMap = Json.fromJson(relation.getAttributes(), Map.class);
                courseItem.setTitle(attributeMap.get(AttributeKey.TITLE));
                courseItem.setType(attributeMap.get(AttributeKey.BIZ_TYPE));
                courseItem.setAppendixPath(attributeMap.get(AttributeKey.APPENDIX_PATH));
                courseItem.setCoverImage("");

                // 设置学习进度
                UserLearningTask learningTask = learningTaskMap.get(relation.getContentId());
                if (learningTask != null) {
                    courseItem.setDuration(null == learningTask.getStudyDuration() ? 0 : learningTask.getStudyDuration());
                    // 设置进度
                    courseItem.setProgress(learningTask.getProgress() != null ? learningTask.getProgress() : 0);
                    
                    // 设置状态
                    String status = LearningStatus.NOT_STARTED;
                    if (learningTask.getStatus() != null) {
                        status = learningTask.getStatus();
                    }
                    courseItem.setStatus(status);
                } else {
                    // 如果没有学习记录，设置默认值
                    courseItem.setProgress(0);
                    courseItem.setStatus(LearningStatus.NOT_STARTED);
                }
                
                courseItem.setRequired(true);
                courseItems.add(courseItem);
            }
        }

        return courseItems;
    }

    /**
     * 获取视频播放信息
     *
     * @param request 视频播放请求
     * @param userId  用户ID
     * @return 视频播放信息
     */
    @Override
    @Transactional
    public VideoPlayResponse getVideoPlayInfo(VideoPlayRequest request, Long userId) {
        // 参数校验
        if (request.getCourseId() == null) {
            throw new RuntimeException("课程ID不能为空");
        }

        // 1. 获取课程信息
        Courses course = getCourseById(request.getCourseId().longValue());
        if (course == null) {
            throw new RuntimeException("课程不存在");
        }

        // 检查课程类型是否为视频
        if (!"video".equals(course.getType())) {
            throw new RuntimeException("该课程不是视频类型");
        }

        // 2. 获取用户播放进度
        Integer lastPosition = 0;

        // 2.1 查询用户学习记录
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, request.getCourseId())
                .eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0);
        UserLearningTask learningTask = userLearningTaskMapper.selectOne(queryWrapper);

        // 如果有学习记录，获取上次播放位置
        if (learningTask != null && learningTask.getStudyDuration() != null) {
            lastPosition = learningTask.getStudyDuration() * 60; // 转换为秒
        }

        // 3. 记录播放开始
        recordPlayStart(request, userId, course);

        // 4. 获取上下课程ID
        Long prevCourseId = getPrevCourseId(course.getId());
        Long nextCourseId = getNextCourseId(course.getId());

        // 5. 构建视频播放响应
        return VideoPlayResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .videoUrl(course.getAppendixPath())
                .coverImage(course.getCoverImage())
                .duration(getDurationFromAttributes(course))
                .progress(lastPosition)
                .lastPosition(lastPosition)
                .prevCourseId(prevCourseId)
                .nextCourseId(nextCourseId)
                .build();
    }

    /**
     * 从课程属性中获取视频时长
     *
     * @param course 课程实体
     * @return 视频时长（秒）
     */
    private Integer getDurationFromAttributes(Courses course) {
        // 如果课程的attributes字段不为空，尝试从中解析视频时长
        if (course.getAttributes() != null && !course.getAttributes().isEmpty()) {
            try {
                // 使用JSON解析attributes字段
                com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSON.parseObject(course.getAttributes());
                if (jsonObject.containsKey("duration")) {
                    return jsonObject.getInteger("duration");
                }
            } catch (Exception e) {
                // 解析失败，使用默认值
                log.warn("解析课程属性中的视频时长失败，使用默认值", e);
            }
        }

        // 默认返回1小时
        return 3600;
    }

    /**
     * 获取上一个课程ID
     *
     * @param courseId 当前课程ID
     * @return 上一个课程ID，如果没有则返回null
     */
    private Long getPrevCourseId(Long courseId) {
        // 实际项目中，应该根据课程的排序或关联关系获取上一个课程
        // 这里简单实现，返回ID小于当前课程的最大ID
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(Courses::getId, courseId)
                .eq(Courses::getIsDel, 0)
                .orderByDesc(Courses::getId)
                .last("LIMIT 1");
        Courses prevCourse = coursesMapper.selectOne(queryWrapper);
        return prevCourse != null ? prevCourse.getId() : null;
    }

    /**
     * 获取下一个课程ID
     *
     * @param courseId 当前课程ID
     * @return 下一个课程ID，如果没有则返回null
     */
    private Long getNextCourseId(Long courseId) {
        // 实际项目中，应该根据课程的排序或关联关系获取下一个课程
        // 这里简单实现，返回ID大于当前课程的最小ID
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.gt(Courses::getId, courseId)
                .eq(Courses::getIsDel, 0)
                .orderByAsc(Courses::getId)
                .last("LIMIT 1");
        Courses nextCourse = coursesMapper.selectOne(queryWrapper);
        return nextCourse != null ? nextCourse.getId() : null;
    }

    /**
     * 记录播放开始
     *
     * @param request 视频播放请求
     * @param userId  用户ID
     * @param course  课程实体
     */
    private void recordPlayStart(VideoPlayRequest request, Long userId, Courses course) {
        // 1. 检查是否已存在课程学习记录
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, course.getId())
                .eq(UserLearningTask::getIsDel, 0);

        UserLearningTask existingTask = userLearningTaskMapper.selectOne(queryWrapper);

        if (existingTask == null) {
            // 如果不存在学习记录，创建新的学习记录
            UserStudyRecordCreateDTO createDTO = new UserStudyRecordCreateDTO();
            createDTO.setUserId(userId);
            createDTO.setBizType(BizType.COURSE);
            createDTO.setBizId(String.valueOf(course.getId()));
            createDTO.setSource("SELF"); // 默认为自学

            // 调用UserStudyService创建学习记录
            userStudyService.createUserStudyRecord(createDTO);
        } else {
            // 如果已存在学习记录，更新学习状态和最后学习时间
            Date now = new Date();

            // 如果状态为未开始，则更新为学习中
            if (LearningStatus.NOT_STARTED.equals(existingTask.getStatus())) {
                existingTask.setStatus(LearningStatus.LEARNING); // 学习中
                existingTask.setStartTime(now);
            }

            existingTask.setLastStudyTime(now);
            existingTask.setGmtModified(now);

            // 获取用户信息
            User user = userMapper.selectById(userId);
            if (user != null) {
                existingTask.setUpdaterId(userId);
                existingTask.setUpdaterName(user.getNickname());
            }

            userLearningTaskMapper.updateById(existingTask);
        }

        // 2. 如果是培训中的视频，确保培训学习记录存在
        if (request.getTrainId() != null) {
            UserStudyRecordCreateDTO trainCreateDTO = new UserStudyRecordCreateDTO();
            trainCreateDTO.setUserId(userId);
            trainCreateDTO.setBizType(BizType.TRAIN);
            trainCreateDTO.setBizId(String.valueOf(request.getTrainId()));
            trainCreateDTO.setSource("SELF"); // 默认为自学，实际会在service中检查是否为指派

            // 调用UserStudyService创建培训学习记录（如果已存在则不会重复创建）
            userStudyService.createUserStudyRecord(trainCreateDTO);
        }

        // 3. 如果是学习地图中的视频，确保学习地图和阶段的学习记录存在
        if (request.getMapId() != null && request.getStageId() != null) {
            // 创建地图学习记录
            UserStudyRecordCreateDTO mapCreateDTO = new UserStudyRecordCreateDTO();
            mapCreateDTO.setUserId(userId);
            mapCreateDTO.setBizType(BizType.LEARNING_MAP);
            mapCreateDTO.setBizId(String.valueOf(request.getMapId()));
            mapCreateDTO.setSource("SELF"); // 默认为自学，实际会在service中检查是否为指派

            // 调用UserStudyService创建学习地图记录（如果已存在则不会重复创建）
            userStudyService.createUserStudyRecord(mapCreateDTO);
        }
    }

    /**
     * 根据ID获取课程
     *
     * @param courseId 课程ID
     * @return 课程实体
     */
    private Courses getCourseById(Long courseId) {
        LambdaQueryWrapper<Courses> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Courses::getId, courseId)
                .eq(Courses::getIsDel, 0); // 未删除的课程
        return coursesMapper.selectOne(queryWrapper);
    }

    /**
     * 获取用户学习进度
     *
     * @param courseId 课程ID
     * @param userId   用户ID
     * @return 用户学习进度DTO
     */
    private CourseDetailResponse.UserProgressDTO getUserProgress(Long userId, Long courseId, Courses course, List<CourseDetailResponse.CourseItemDTO> courseItems) {
        RecordLearningProgressRequest request = new RecordLearningProgressRequest();
        request.setContentType(BizType.COURSE);
        request.setContentId(courseId);
        UserLearningTask courseTask = userStudyService.getOrCreateLearningTask(userId, request);

        if (!"series".equals(course.getType())) {
            return CourseDetailResponse.UserProgressDTO.builder()
                    .status(courseTask.getStatus())
                    .progress(courseTask.getProgress())
                    .studyDuration(courseTask.getStudyDuration())
                    .lastStudyTime(JinxDateUtil.formatDateToString(courseTask.getLastStudyTime()))
                    .build();
        }

        // 确定学习状态和进度
        int totalDuration = 0;

        long total = Math.max(courseItems.stream()
                .filter(v -> Boolean.TRUE.equals(v.getRequired()))
                .count(), 1);
        long completed = courseItems.stream()
                .filter(v -> Boolean.TRUE.equals(v.getRequired()) && v.getProgress() == 100)
                .count();
        for (CourseDetailResponse.CourseItemDTO courseItem : courseItems) {
            totalDuration += null == courseItem.getDuration() ? 0 : courseItem.getDuration();
        }

        // 开始计算
        int progress = (int) (completed / total * 100);
        courseTask.setProgress(progress);
        String statusStr = courseTask.convertStatus(progress);

        return CourseDetailResponse.UserProgressDTO.builder()
                .status(statusStr)
                .progress(progress)
                .studyDuration(totalDuration)
                .lastStudyTime(JinxDateUtil.formatDateToString(courseTask.getLastStudyTime()))
                .build();
    }

    /**
     * 获取学习人员列表
     *
     * @param courseId 课程ID
     * @return 学习人员列表
     */
    private List<CourseDetailResponse.LearnerDTO> getLearners(Long courseId) {
        // 查询学习记录
        LambdaQueryWrapper<UserLearningTask> recordsWrapper = new LambdaQueryWrapper<>();
        recordsWrapper.eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, courseId)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> learningTasks = userLearningTaskMapper.selectList(recordsWrapper);

        if (learningTasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取用户ID列表
        List<Long> userIds = learningTasks.stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());

        // 查询用户信息
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getUserId, userIds)
                .eq(User::getIsDel, 0);
        List<User> users = userMapper.selectList(userWrapper);

        // 查询用户部门信息
        Map<Long, String> userDepartmentMap = getUserDepartments(userIds);

        // 构建学习人员列表
        return users.stream().map(user -> {
            // 查找该用户的学习记录
            UserLearningTask userTask = learningTasks.stream()
                    .filter(task -> task.getUserId().equals(user.getUserId()))
                    .findFirst()
                    .orElse(null);

            // 确定学习状态
            String status;
            if (userTask == null || userTask.getStatus() == null) {
                status = LearningStatus.NOT_STARTED;
            } else {
                status = userTask.getStatus();
            }

            // 获取用户部门信息
            String department = userDepartmentMap.getOrDefault(user.getUserId(), "");

            return CourseDetailResponse.LearnerDTO.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .department(department)
                    .status(status)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户部门信息
     *
     * @param userIds 用户ID列表
     * @return 用户ID到部门名称的映射
     */
    private Map<Long, String> getUserDepartments(List<Long> userIds) {
        Map<Long, String> userDepartmentMap = new HashMap<>();

        // 查询用户部门关联
        LambdaQueryWrapper<DepartmentUser> duWrapper = new LambdaQueryWrapper<>();
        duWrapper.in(DepartmentUser::getUserId, userIds)
                .eq(DepartmentUser::getIsDel, 0);
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duWrapper);

        if (departmentUsers.isEmpty()) {
            return userDepartmentMap;
        }

        // 获取部门ID列表
        List<Long> departmentIds = departmentUsers.stream()
                .map(DepartmentUser::getDepartmentId)
                .collect(Collectors.toList());

        // 查询部门信息
        LambdaQueryWrapper<Department> departmentWrapper = new LambdaQueryWrapper<>();
        departmentWrapper.in(Department::getId, departmentIds)
                .eq(Department::getIsDel, 0);
        List<Department> departments = departmentMapper.selectList(departmentWrapper);

        // 构建部门ID到部门名称的映射
        Map<Long, String> departmentNameMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));

        // 构建用户ID到部门名称的映射
        for (DepartmentUser du : departmentUsers) {
            String departmentName = departmentNameMap.get(du.getDepartmentId());
            if (departmentName != null) {
                userDepartmentMap.put(du.getUserId(), departmentName);
            }
        }

        return userDepartmentMap;
    }

    /**
     * 构建课程详情响应
     *
     * @param course       课程实体
     * @param userProgress 用户学习进度
     * @param learners     学习人员列表
     * @param courseItems  课程章节列表
     * @return 课程详情响应
     */
    private CourseDetailResponse buildCourseDetailResponse(Courses course, CourseDetailResponse.UserProgressDTO userProgress,
                                                           List<CourseDetailResponse.LearnerDTO> learners,
                                                           List<CourseDetailResponse.CourseItemDTO> courseItems) {
        // 构建讲师信息
        CourseDetailResponse.InstructorDTO instructor = new CourseDetailResponse.InstructorDTO();
        if (course.getInstructorId() != null) {
            User user = userMapper.selectById(course.getInstructorId());
            instructor = CourseDetailResponse.InstructorDTO.builder()
                    .id(course.getInstructorId())
                    .name(Objects.nonNull(user) ? user.getNickname() : "")
                    .avatar(Objects.nonNull(user) ? user.getAvatar() : "") // 讲师头像需要从其他表获取，这里暂时留空
                    .build();
        }
        if (org.apache.commons.lang3.StringUtils.isBlank(instructor.getName())) {
            instructor.setName(course.getGuestName());
        }

        // 构建课程详情响应
        CourseDetailResponse response = CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .type(course.getType())
                .coverImage(course.getCoverImage())
                .description(course.getDescription())
                .credit(course.getCredit())
                .instructor(instructor)
                .appendixType(course.getAppendixType())
                .appendixPath(course.getAppendixPath())
                .duration(getDurationFromAttributes(course)) // 课程时长，从课程属性中获取
                .userProgress(userProgress)
                .learners(learners)
                .courseItems(courseItems)
                .build();

        // 扩展实现
        if (StringUtils.hasText(course.getAttributes())) {
            Map<String, Object> attributeMap = Json.fromJson(course.getAttributes(), Map.class);
            response.setDuration(Objects.nonNull(attributeMap.get("duration")) ? Integer.valueOf(attributeMap.get("duration").toString()) : null);
        }
        return response;
    }
}

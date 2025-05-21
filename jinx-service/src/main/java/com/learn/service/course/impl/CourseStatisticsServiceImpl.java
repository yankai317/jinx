package com.learn.service.course.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.course.CourseStatisticsDTO;
import com.learn.dto.course.CourseLearnerRequest;
import com.learn.dto.course.CourseLearnerResponse;
import com.learn.infrastructure.repository.entity.*;
import com.learn.infrastructure.repository.mapper.*;
import com.learn.service.course.CourseStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程统计服务实现类
 */
@Service
@Slf4j
public class CourseStatisticsServiceImpl implements CourseStatisticsService {

    @Autowired
    private CoursesMapper coursesMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 获取课程统计数据
     *
     * @param courseId 课程ID
     * @return 课程统计数据
     */
    @Override
    public CourseStatisticsDTO getCourseStatistics(Long courseId) {
        // 查询课程信息
        Courses course = coursesMapper.selectById(courseId);
        if (course == null) {
            log.error("课程不存在, courseId: {}", courseId);
            return null;
        }

        // 查询学习记录
        LambdaQueryWrapper<UserLearningTask> recordsWrapper = new LambdaQueryWrapper<>();
        recordsWrapper.eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, courseId)
                .eq(UserLearningTask::getIsDel, 0);
        List<UserLearningTask> records = userLearningTaskMapper.selectList(recordsWrapper);

        // 计算统计数据
        Integer viewCount = course.getViewCount() != null ? course.getViewCount() : 0;
        
        // 计算完成人数
        Integer completeCount = 0;
        if (!records.isEmpty()) {
            completeCount = (int) records.stream()
                    .filter(record -> record.getStatus() != null && LearningStatus.COMPLETED.equals(record.getStatus())) // 状态为已完成
                    .count();
        }
        
        // 计算总学习时长和平均学习时长
        Integer totalDuration = 0;
        Integer avgDuration = 0;
        if (!records.isEmpty()) {
            totalDuration = records.stream()
                    .filter(record -> record.getStudyDuration() != null)
                    .mapToInt(UserLearningTask::getStudyDuration)
                    .sum();
            avgDuration = records.isEmpty() ? 0 : totalDuration / records.size();
        }

        // 获取部门统计数据
        List<CourseStatisticsDTO.DepartmentStatDTO> departmentStats = getDepartmentStats(records);

        // 获取时间分布数据
        List<CourseStatisticsDTO.TimeDistributionDTO> timeDistribution = getTimeDistribution(records);

        // 构建返回结果
        return CourseStatisticsDTO.builder()
                .viewCount(viewCount)
                .completeCount(completeCount)
                .avgDuration(avgDuration)
                .totalDuration(totalDuration)
                .departmentStats(departmentStats)
                .timeDistribution(timeDistribution)
                .build();
    }

    /**
     * 获取课程学习人员列表
     *
     * @param courseId 课程ID
     * @param request  请求参数
     * @return 学习人员列表
     */
    @Override
    public CourseLearnerResponse getCourseLearners(Long courseId, CourseLearnerRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, courseId)
                .eq(UserLearningTask::getIsDel, 0);

        // 按部门筛选
        if (request.getDepartmentId() != null) {
            // 查询部门下的用户ID列表
            LambdaQueryWrapper<DepartmentUser> duWrapper = new LambdaQueryWrapper<>();
            duWrapper.eq(DepartmentUser::getDepartmentId, request.getDepartmentId())
                    .eq(DepartmentUser::getIsDel, 0);
            List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duWrapper);
            List<Long> userIds = departmentUsers.stream()
                    .map(DepartmentUser::getUserId)
                    .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                wrapper.in(UserLearningTask::getUserId, userIds);
            } else {
                // 如果部门下没有用户，返回空结果
                return new CourseLearnerResponse();
            }
        }

        // 按关键词筛选（用户名/工号）
        if (StringUtils.hasText(request.getKeyword())) {
            // 查询匹配关键词的用户ID列表
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.like(User::getNickname, request.getKeyword())
                    .or()
                    .like(User::getEmployeeNo, request.getKeyword())
                    .eq(User::getIsDel, 0);
            List<User> users = userMapper.selectList(userWrapper);
            List<Long> userIds = users.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toList());
            
            if (!userIds.isEmpty()) {
                wrapper.in(UserLearningTask::getUserId, userIds);
            } else {
                // 如果没有匹配的用户，返回空结果
                return new CourseLearnerResponse();
            }
        }

        // 按状态筛选
        if ("completed".equals(request.getStatus())) {
            wrapper.eq(UserLearningTask::getStatus, 2); // 已完成
        } else if ("learning".equals(request.getStatus())) {
            wrapper.eq(UserLearningTask::getStatus, 1); // 学习中
        }

        // 分页查询
        Page<UserLearningTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<UserLearningTask> taskPage = userLearningTaskMapper.selectPage(page, wrapper);

        // 构建返回结果
        CourseLearnerResponse response = new CourseLearnerResponse();
        response.setTotal((int) taskPage.getTotal());
        
        if (taskPage.getRecords().isEmpty()) {
            response.setList(new ArrayList<>());
            return response;
        }

        // 获取用户ID列表
        List<Long> userIds = taskPage.getRecords().stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());

        // 查询用户信息
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getUserId, userIds)
                .eq(User::getIsDel, 0);
        List<User> users = userMapper.selectList(userWrapper);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getUserId, user -> user));

        // 查询用户部门信息
        LambdaQueryWrapper<DepartmentUser> duWrapper = new LambdaQueryWrapper<>();
        duWrapper.in(DepartmentUser::getUserId, userIds)
                .eq(DepartmentUser::getIsDel, 0);
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duWrapper);
        
        // 构建用户ID到部门ID的映射
        Map<Long, Long> userDepartmentMap = departmentUsers.stream()
                .collect(Collectors.toMap(DepartmentUser::getUserId, DepartmentUser::getDepartmentId, (v1, v2) -> v1));
        
        // 查询部门信息
        Set<Long> departmentIds = departmentUsers.stream()
                .map(DepartmentUser::getDepartmentId)
                .collect(Collectors.toSet());
        
        LambdaQueryWrapper<Department> departmentWrapper = new LambdaQueryWrapper<>();
        departmentWrapper.in(Department::getId, departmentIds)
                .eq(Department::getIsDel, 0);
        List<Department> departments = departmentMapper.selectList(departmentWrapper);
        
        // 构建部门ID到部门名称的映射
        Map<Long, String> departmentMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));

        // 构建学习人员列表
        List<CourseLearnerResponse.LearnerDTO> learnerList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        for (UserLearningTask task : taskPage.getRecords()) {
            User user = userMap.get(task.getUserId());
            if (user == null) {
                continue;
            }
            
            CourseLearnerResponse.LearnerDTO learner = new CourseLearnerResponse.LearnerDTO();
            learner.setUserId(user.getUserId());
            learner.setUserName(user.getNickname());
            learner.setEmployeeNo(user.getEmployeeNo());
            
            // 设置部门
            Long departmentId = userDepartmentMap.get(user.getUserId());
            if (departmentId != null) {
                learner.setDepartment(departmentMap.get(departmentId));
            }
            
            // 设置学习时长
            learner.setStudyDuration(task.getStudyDuration());
            
            // 设置是否完成
            learner.setCompleted(task.getStatus() != null && LearningStatus.COMPLETED.equals(task.getStatus()));
            
            // 设置最后学习时间
            if (task.getLastStudyTime() != null) {
                learner.setLastStudyTime(dateFormat.format(task.getLastStudyTime()));
            }
            
            learnerList.add(learner);
        }
        
        response.setList(learnerList);
        return response;
    }

    /**
     * 获取部门统计数据
     *
     * @param records 学习记录列表
     * @return 部门统计数据
     */
    private List<CourseStatisticsDTO.DepartmentStatDTO> getDepartmentStats(List<UserLearningTask> records) {
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取用户ID列表
        List<Long> userIds = records.stream()
                .map(UserLearningTask::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 查询用户部门信息
        LambdaQueryWrapper<DepartmentUser> duWrapper = new LambdaQueryWrapper<>();
        duWrapper.in(DepartmentUser::getUserId, userIds)
                .eq(DepartmentUser::getIsDel, 0);
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duWrapper);

        // 获取部门ID列表
        List<Long> departmentIds = departmentUsers.stream()
                .map(DepartmentUser::getDepartmentId)
                .distinct()
                .collect(Collectors.toList());

        if (departmentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询部门信息
        LambdaQueryWrapper<Department> departmentWrapper = new LambdaQueryWrapper<>();
        departmentWrapper.in(Department::getId, departmentIds)
                .eq(Department::getIsDel, 0);
        List<Department> departments = departmentMapper.selectList(departmentWrapper);

        // 构建部门ID到部门名称的映射
        Map<Long, String> departmentMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));

        // 构建用户ID到部门ID的映射
        Map<Long, Long> userDepartmentMap = departmentUsers.stream()
                .collect(Collectors.toMap(DepartmentUser::getUserId, DepartmentUser::getDepartmentId, (v1, v2) -> v1));

        // 统计各部门的学习人数
        Map<Long, Integer> departmentCountMap = new HashMap<>();
        for (UserLearningTask record : records) {
            Long departmentId = userDepartmentMap.get(record.getUserId());
            if (departmentId != null) {
                departmentCountMap.put(departmentId, departmentCountMap.getOrDefault(departmentId, 0) + 1);
            }
        }

        // 构建部门统计数据
        List<CourseStatisticsDTO.DepartmentStatDTO> departmentStats = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : departmentCountMap.entrySet()) {
            String departmentName = departmentMap.get(entry.getKey());
            if (departmentName != null) {
                CourseStatisticsDTO.DepartmentStatDTO departmentStat = CourseStatisticsDTO.DepartmentStatDTO.builder()
                        .name(departmentName)
                        .count(entry.getValue())
                        .build();
                departmentStats.add(departmentStat);
            }
        }

        return departmentStats;
    }

    /**
     * 获取时间分布数据
     *
     * @param records 学习记录列表
     * @return 时间分布数据
     */
    private List<CourseStatisticsDTO.TimeDistributionDTO> getTimeDistribution(List<UserLearningTask> records) {
        if (records.isEmpty()) {
            return new ArrayList<>();
        }

        // 按日期分组统计
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Integer> dateCountMap = new HashMap<>();
        
        for (UserLearningTask record : records) {
            if (record.getStartTime() != null) {
                String date = dateFormat.format(record.getStartTime());
                dateCountMap.put(date, dateCountMap.getOrDefault(date, 0) + 1);
            } else if (record.getLastStudyTime() != null) {
                // 如果没有开始时间，则使用最后学习时间
                String date = dateFormat.format(record.getLastStudyTime());
                dateCountMap.put(date, dateCountMap.getOrDefault(date, 0) + 1);
            }
        }

        // 构建时间分布数据
        List<CourseStatisticsDTO.TimeDistributionDTO> timeDistribution = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : dateCountMap.entrySet()) {
            CourseStatisticsDTO.TimeDistributionDTO timeDistributionDTO = CourseStatisticsDTO.TimeDistributionDTO.builder()
                    .date(entry.getKey())
                    .count(entry.getValue())
                    .build();
            timeDistribution.add(timeDistributionDTO);
        }

        // 按日期排序
        timeDistribution.sort(Comparator.comparing(CourseStatisticsDTO.TimeDistributionDTO::getDate));

        return timeDistribution;
    }
}

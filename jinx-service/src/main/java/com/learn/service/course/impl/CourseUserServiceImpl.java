package com.learn.service.course.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.common.exception.CommonException;
import com.learn.constants.BizType;
import com.learn.constants.LearningStatus;
import com.learn.dto.common.AssignRequest;
import com.learn.dto.common.AssignResponse;
import com.learn.dto.course.CourseAssignRequest;
import com.learn.dto.course.CourseAssignResponse;
import com.learn.dto.course.CourseLearnerRequest;
import com.learn.dto.course.CourseLearnerResponse;
import com.learn.dto.user.UserStudyRecordCreateDTO;
import com.learn.infrastructure.repository.entity.AssignmentDetail;
import com.learn.infrastructure.repository.entity.CommonRange;
import com.learn.infrastructure.repository.entity.Courses;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.AssignmentDetailMapper;
import com.learn.infrastructure.repository.mapper.CommonRangeMapper;
import com.learn.infrastructure.repository.mapper.CoursesMapper;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeCreateResponse;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.course.CourseUserService;
import com.learn.service.impl.CommonRangeInterfaceImpl;
import com.learn.service.user.UserStudyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程用户服务实现类
 */
@Service
@Slf4j
public class CourseUserServiceImpl implements CourseUserService {

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private DepartmentMapper departmentMapper;
    
    @Autowired
    private DepartmentUserMapper departmentUserMapper;
    
    @Autowired
    private CoursesMapper coursesMapper;
    
    @Autowired
    private CommonRangeInterfaceImpl commonRangeInterface;
    
    /**
     * 获取课程学习人员列表
     *
     * @param courseId 课程ID
     * @param request 查询条件
     * @return 学习人员列表
     */
    @Override
    public CourseLearnerResponse getCourseLearners(Long courseId, CourseLearnerRequest request) {
        log.info("获取课程学习人员列表，课程ID：{}，请求参数：{}", courseId, request);
        
        // 设置默认分页参数
        if (request.getPageNum() == null || request.getPageNum() < 1) {
            request.setPageNum(1);
        }
        
        if (request.getPageSize() == null || request.getPageSize() < 1) {
            request.setPageSize(10);
        }
        
        // 查询学习任务
        LambdaQueryWrapper<UserLearningTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(UserLearningTask::getBizType, BizType.COURSE)
                .eq(UserLearningTask::getBizId, courseId)
                .eq(UserLearningTask::getIsDel, 0);
        if (Objects.nonNull(request.getUserId())) {
            taskWrapper.eq(UserLearningTask::getUserId, request.getUserId());
        }
        if (org.apache.commons.lang3.StringUtils.isNotBlank(request.getStatus()) && !request.getStatus().equals("all")) {
            taskWrapper.eq(UserLearningTask::getStatus, request.getStatus());
        }
        
        // 按部门筛选
        List<Long> userIds = null;
        if (request.getDepartmentId() != null) {
            // 查询部门下的所有用户ID
            LambdaQueryWrapper<DepartmentUser> duWrapper = new LambdaQueryWrapper<>();
            duWrapper.eq(DepartmentUser::getDepartmentId, request.getDepartmentId())
                    .eq(DepartmentUser::getIsDel, 0);
            List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duWrapper);
            
            if (departmentUsers != null && !departmentUsers.isEmpty()) {
                userIds = departmentUsers.stream()
                        .map(DepartmentUser::getUserId)
                        .collect(Collectors.toList());
                
                taskWrapper.in(UserLearningTask::getUserId, userIds);
            } else {
                // 如果部门下没有用户，则返回空结果
                CourseLearnerResponse response = new CourseLearnerResponse();
                response.setTotal(0);
                response.setList(new ArrayList<>());
                return response;
            }
        }

        // 分页查询
        Page<UserLearningTask> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<UserLearningTask> taskPage = userLearningTaskMapper.selectPage(page, taskWrapper);
        
        // 构建响应对象
        CourseLearnerResponse response = new CourseLearnerResponse();
        response.setTotal(Math.toIntExact(taskPage.getTotal()));
        
        if (taskPage.getRecords() == null || taskPage.getRecords().isEmpty()) {
            response.setList(new ArrayList<>());
            return response;
        }
        
        // 获取所有用户ID
        List<Long> taskUserIds = taskPage.getRecords().stream()
                .map(UserLearningTask::getUserId)
                .collect(Collectors.toList());
                
        // 查询用户信息
        LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
        userQueryWrapper.in(User::getUserId, taskUserIds)
                .eq(User::getIsDel, 0);
        List<User> users = userMapper.selectList(userQueryWrapper);
        Map<Long, User> userMap = new HashMap<>();
        if (users != null) {
            userMap = users.stream().collect(Collectors.toMap(User::getUserId, user -> user));
        }
        
        // 查询用户部门信息
        LambdaQueryWrapper<DepartmentUser> departmentUserWrapper = new LambdaQueryWrapper<>();
        departmentUserWrapper.in(DepartmentUser::getUserId, taskUserIds)
                .eq(DepartmentUser::getIsDel, 0);
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(departmentUserWrapper);
        
        // 构建用户ID到部门ID的映射
        Map<Long, Long> userDepartmentMap = new HashMap<>();
        if (departmentUsers != null) {
            userDepartmentMap = departmentUsers.stream()
                    .collect(Collectors.toMap(DepartmentUser::getUserId, DepartmentUser::getDepartmentId, (v1, v2) -> v1));
        }
        
        // 查询部门信息
        List<Long> departmentIds = new ArrayList<>(userDepartmentMap.values());
        Map<Long, String> departmentNameMap = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            LambdaQueryWrapper<Department> departmentWrapper = new LambdaQueryWrapper<>();
            departmentWrapper.in(Department::getId, departmentIds)
                    .eq(Department::getIsDel, 0);
            List<Department> departments = departmentMapper.selectList(departmentWrapper);
            if (departments != null) {
                departmentNameMap = departments.stream()
                        .collect(Collectors.toMap(Department::getId, Department::getDepartmentName));
            }
        }
        
        // 构建学习人员列表
        List<CourseLearnerResponse.LearnerDTO> learnerList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // 按完成状态筛选
        for (UserLearningTask task : taskPage.getRecords()) {
            CourseLearnerResponse.LearnerDTO learner = new CourseLearnerResponse.LearnerDTO();
            learner.setUserId(task.getUserId());
            
            // 设置用户名和工号
            User user = userMap.get(task.getUserId());
            if (user != null) {
                learner.setUserName(user.getNickname());
                learner.setEmployeeNo(user.getEmployeeNo());
            }
            
            // 设置部门
            Long departmentId = userDepartmentMap.get(task.getUserId());
            if (departmentId != null) {
                learner.setDepartment(departmentNameMap.get(departmentId));
            }
            
            // 设置学习时长
            learner.setStudyDuration(task.getStudyDuration());
            learner.setStatus(task.getStatus());
            
            // 设置是否完成
            learner.setCompleted(task.getStatus() != null && task.getStatus().equals(LearningStatus.COMPLETED));
            learner.setProgress(task.getProgress() == null ? 0 : task.getProgress());
            // 设置最后学习时间
            if (task.getLastStudyTime() != null) {
                learner.setLastStudyTime(dateFormat.format(task.getLastStudyTime()));
            }
            if (task.getCompletionTime() != null) {
                learner.setCompletionTime(dateFormat.format(task.getCompletionTime()));
            }
            if (task.getStartTime() != null) {
                learner.setStartTime(dateFormat.format(task.getStartTime()));
            }
            
            learnerList.add(learner);
        }
        
        response.setList(learnerList);
        return response;
    }
    
    /**
     * 指派课程给用户
     *
     * @param request 指派课程请求
     * @return 指派结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AssignResponse assignCourse(AssignRequest request) {
        log.info("指派课程给用户，请求参数：{}", request);
        
        // 参数校验
        if (request.getBizId() == null) {
            throw new CommonException("课程ID不能为空");
        }
        
        if (CollectionUtils.isEmpty(request.getUserIds())) {
            throw new CommonException("用户ID列表不能为空");
        }
        
        // 默认发送通知
        if (request.getSendNotification() == null) {
            request.setSendNotification(true);
        }
        
        // 查询课程是否存在
        Courses course = coursesMapper.selectById(request.getBizId());
        if (course == null || course.getIsDel() == 1) {
            throw new CommonException("课程不存在或已删除");
        }
        
        // 校验操作权限
        checkOperationPermission(request.getOperatorId(), course.getId());
        

        // 创建通用范围记录（任务指派）
        CommonRangeCreateRequest rangeRequest = new CommonRangeCreateRequest();
        rangeRequest.setModelType("assignment"); // 指派类型
        rangeRequest.setType(BizType.COURSE); // 课程类型
        rangeRequest.setTypeId(course.getId());
        rangeRequest.setCreatorId(request.getOperatorId());
        rangeRequest.setCreatorName(request.getOperatorName());
        
        // 设置目标范围
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        // 添加用户ID
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            targetTypeAndIds.put("user", request.getUserIds());
        }
        
        // 添加角色ID
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            targetTypeAndIds.put("role", request.getRoleIds());
        }
        
        // 添加部门ID
        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            targetTypeAndIds.put("department", request.getDepartmentIds());
        }
        
        rangeRequest.setTargetTypeAndIds(targetTypeAndIds);
        
        // 创建范围记录
        try {
            CommonRangeCreateResponse rangeResponse = commonRangeInterface.batchCreateRange(rangeRequest);
            if (rangeResponse == null || !rangeResponse.getSuccess() || CollectionUtils.isEmpty(rangeResponse.getRangeIds())) {
                throw new CommonException("创建课程指派范围记录失败");
            }

            // 构建响应
            AssignResponse response = new AssignResponse();
            response.setRangeIds(rangeResponse.getRangeIds()); // 设置范围ID
            
            return response;
        } catch (Exception e) {
            log.error("创建课程指派范围记录失败", e);
            throw new CommonException("创建课程指派记录失败：" + e.getMessage());
        }
    }
    
    /**
     * 校验操作权限
     *
     * @param userId 用户ID
     * @param courseId 课程ID
     */
    private void checkOperationPermission(Long userId, Long courseId) {
        if (userId == null) {
            throw new CommonException("用户未登录");
        }
        
        // 构建权限校验请求
        CommonRangeQueryRequest request = new CommonRangeQueryRequest();
        request.setUserId(userId.toString());
        request.setModelType("manage"); // 管理权限
        request.setType(BizType.COURSE); // 课程类型
        request.setTypeId(courseId);
        
        // 查询用户信息，获取用户的角色和部门
        Map<String, List<Long>> targetTypeAndIds = new HashMap<>();
        List<Long> userIds = new ArrayList<>();
        userIds.add(userId);
        targetTypeAndIds.put("user", userIds);
        
        try {
            // 获取用户的部门和角色
            User user = userMapper.selectById(userId);
            if (user != null) {
                // 查询用户的部门
                LambdaQueryWrapper<DepartmentUser> duWrapper = new LambdaQueryWrapper<>();
                duWrapper.eq(DepartmentUser::getUserId, userId)
                        .eq(DepartmentUser::getIsDel, 0);
                List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(duWrapper);
                
                if (!CollectionUtils.isEmpty(departmentUsers)) {
                    List<Long> departmentIds = departmentUsers.stream()
                            .map(DepartmentUser::getDepartmentId)
                            .collect(Collectors.toList());
                    targetTypeAndIds.put("department", departmentIds);
                }
                
                // TODO: 查询用户的角色，这里需要根据实际情况实现
            }
            
            request.setTargetTypeAndIds(targetTypeAndIds);
            
            // 检查用户是否有权限
            commonRangeInterface.checkUserHasRightsIfNotThrowException(request);

        } catch (Exception e) {
            log.error("校验操作权限失败", e);
            throw new CommonException("校验操作权限失败：" + e.getMessage());
        }
    }

}

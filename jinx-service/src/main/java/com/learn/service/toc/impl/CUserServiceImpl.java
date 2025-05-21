package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.toc.user.UserInfoResponse;
import com.learn.constants.BizType;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserCertificate;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserCertificateMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.toc.CUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * C端用户服务实现类
 */
@Service
@Slf4j
public class CUserServiceImpl implements CUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    @Autowired
    private UserCertificateMapper userCertificateMapper;

    /**
     * 获取当前登录用户信息
     *1. 查询用户基本信息
     * 2. 查询用户部门信息
     * 3. 查询用户学习统计数据
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        try {
            // 1. 查询用户基本信息
            User user = getUserById(userId);
            if (user == null) {
                log.error("用户不存在, userId: {}", userId);
                return UserInfoResponse.error("用户不存在");
            }

            // 2. 查询用户部门信息
            String departmentName = getUserDepartmentName(userId);

            // 3. 查询用户学习统计数据
            // 3.1 查询总学习时长
            Integer totalStudyTime = getTotalStudyTime(userId);

            // 3.2 查询已完成培训数量
            Integer completedTrainCount = getCompletedTrainCount(userId);// 3.3 查询已完成学习地图数量
            Integer completedMapCount = getCompletedMapCount(userId);

            // 3.4 查询获得证书数量
            Integer certificateCount = getCertificateCount(userId);

            // 3.5 计算总学分(假设每完成一个培训获得10学分，每完成一个学习地图获得20学分)
            Integer totalCredit = completedTrainCount * 10 + completedMapCount * 20;

            // 4. 构建用户等级 (假设根据总学分计算等级，每100学分提升一级，从1级开始)
            Integer level = totalCredit / 100 + 1;

            // 5. 构建用户信息响应
            UserInfoResponse.UserInfoData userData = UserInfoResponse.UserInfoData.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .departmentName(departmentName)
                    .level(level)
                    .totalCredit(totalCredit)
                    .totalStudyTime(totalStudyTime)
                    .completedTrainCount(completedTrainCount)
                    .completedMapCount(completedMapCount)
                    .certificateCount(certificateCount)
                    .build();

            return UserInfoResponse.success(userData);
        } catch (Exception e) {
            log.error("获取用户信息异常, userId: {}", userId, e);
            return UserInfoResponse.error("获取用户信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    private User getUserById(Long userId) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getUserId, userId)
                .eq(User::getIsDel, 0);
        return userMapper.selectOne(userWrapper);
    }

    /**
     * 获取用户部门名称
     *
     * @param userId 用户ID
     * @return 部门名称
     */
    private String getUserDepartmentName(Long userId) {
        // 查询用户部门关联
        LambdaQueryWrapper<DepartmentUser> departmentUserWrapper = new LambdaQueryWrapper<>();
        departmentUserWrapper.eq(DepartmentUser::getUserId, userId)
                .eq(DepartmentUser::getIsDel, 0);

        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(departmentUserWrapper);
        if (departmentUsers.isEmpty()) {
            return "";
        }

        // 获取第一个部门信息
        Long departmentId = departmentUsers.get(0).getDepartmentId();
        LambdaQueryWrapper<Department> departmentWrapper = new LambdaQueryWrapper<>();
        departmentWrapper.eq(Department::getId, departmentId)
                .eq(Department::getIsDel, 0);

        Department department = departmentMapper.selectOne(departmentWrapper);
        return department != null ? department.getDepartmentName() : "";
    }

    /**
     * 获取用户总学习时长
     *
     * @param userId 用户ID
     * @return 总学习时长（分钟）
     */
    private Integer getTotalStudyTime(Long userId) {
        // 查询用户所有学习任务的学习时长
        LambdaQueryWrapper<UserLearningTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getIsDel, 0);
        
        List<UserLearningTask> userLearningTasks = userLearningTaskMapper.selectList(queryWrapper);
        
        // 计算总学习时长
        int totalStudyTime = userLearningTasks.stream()
                .mapToInt(UserLearningTask::getStudyDuration)
                .sum();
        
        return totalStudyTime;
    }

    /**
     * 获取用户已完成培训数量
     *
     * @param userId 用户ID
     * @return 已完成培训数量
     */
    private Integer getCompletedTrainCount(Long userId) {
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, BizType.TRAIN) // 培训类型
                .eq(UserLearningTask::getStatus, 2) // 2-已完成
                .eq(UserLearningTask::getIsDel, 0);
        return userLearningTaskMapper.selectCount(wrapper).intValue();
    }

    /**
     * 获取用户已完成学习地图数量
     *
     * @param userId 用户ID
     * @return 已完成学习地图数量
     */
    private Integer getCompletedMapCount(Long userId) {
        LambdaQueryWrapper<UserLearningTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningTask::getUserId, userId)
                .eq(UserLearningTask::getBizType, BizType.LEARNING_MAP) // 学习地图类型
                .eq(UserLearningTask::getStatus, 2) // 2-已完成
                .eq(UserLearningTask::getIsDel, 0);
        return userLearningTaskMapper.selectCount(wrapper).intValue();
    }

    /**
     * 获取用户获得证书数量
     *
     * @param userId 用户ID
     * @return 获得证书数量
     */
    private Integer getCertificateCount(Long userId) {
        LambdaQueryWrapper<UserCertificate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCertificate::getUserId, userId)
                .eq(UserCertificate::getStatus, 0) // 0-有效
                .eq(UserCertificate::getIsDel, 0);
        return userCertificateMapper.selectCount(wrapper).intValue();
    }
}

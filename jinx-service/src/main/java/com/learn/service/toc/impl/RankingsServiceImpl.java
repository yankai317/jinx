package com.learn.service.toc.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.constants.LearningStatus;
import com.learn.dto.toc.rankings.RankingsResponse;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserLearningTask;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.UserLearningTaskMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.toc.RankingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排行榜服务实现类
 */
@Service
@Slf4j
public class RankingsServiceImpl implements RankingsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private UserLearningTaskMapper userLearningTaskMapper;

    /**
     * 获取学习排行榜数据
     *
     * @param type   排行榜类型：all-全员，department-部门，默认全员
     * @param limit  返回数量限制，默认10
     * @param userId 当前用户ID
     * @return 排行榜数据
     */
    @Override
    public RankingsResponse getRankings(String type, Integer limit, Long userId) {
        return getRankings(type, limit, userId, null);
    }

    /**
     * 获取学习排行榜数据
     *
     * @param type         排行榜类型：all-全员，department-部门，默认全员
     * @param limit        返回数量限制，默认10
     * @param userId       当前用户ID
     * @param departmentId 部门ID，type=department时使用
     * @return 排行榜数据
     */
    @Override
    public RankingsResponse getRankings(String type, Integer limit, Long userId, Long departmentId) {
        // 设置默认值
        if (type == null || type.isEmpty()) {
            type = "all";
        }
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        // 获取用户部门信息
        Long userDepartmentId = departmentId;
        if ("department".equals(type) && userDepartmentId == null) {
            userDepartmentId = getUserDepartmentId(userId);
            if (userDepartmentId == null) {
                // 如果用户没有部门，则返回全员排行榜
                type = "all";
            }
        }

        // 计算所有用户的学习积分
        Map<Long, Integer> userCreditMap = calculateUserCredits();

        // 根据类型筛选用户
        List<Long> filteredUserIds;
        if ("department".equals(type) && userDepartmentId != null) {
            // 部门排行榜
            filteredUserIds = getUserIdsByDepartment(userDepartmentId);
        } else {
            // 全员排行榜
            filteredUserIds = new ArrayList<>(userCreditMap.keySet());
        }

        // 按积分排序
        List<Map.Entry<Long, Integer>> sortedEntries = userCreditMap.entrySet().stream()
                .filter(entry -> filteredUserIds.contains(entry.getKey()))
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());

        // 构建排行榜响应
        RankingsResponse response = new RankingsResponse();
        response.setType(type);

        // 获取前limit名用户
        List<RankingsResponse.RankingItem> rankingItems = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, Integer> entry : sortedEntries) {
            if (rankingItems.size() >= limit) {
                break;
            }

            Long uid = entry.getKey();
            Integer credit = entry.getValue();

            // 获取用户信息
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUserId, uid)
                            .eq(User::getIsDel, 0)
            );

            if (user == null) {
                continue;
            }

            // 获取用户部门
            String departmentName = getUserDepartmentName(uid);

            // 构建排行项
            RankingsResponse.RankingItem item = new RankingsResponse.RankingItem();
            item.setRank(rank++);
            item.setUserId(uid);
            item.setNickname(user.getNickname());
            item.setAvatar(user.getAvatar());
            item.setDepartment(departmentName);
            item.setCredit(credit);

            rankingItems.add(item);
        }
        response.setList(rankingItems);

        // 获取当前用户排名信息
        if (userId != null) {
            RankingsResponse.UserRank userRank = new RankingsResponse.UserRank();
            
            // 查找用户在排序列表中的位置
            int userRankPosition = -1;
            for (int i = 0; i < sortedEntries.size(); i++) {
                if (sortedEntries.get(i).getKey().equals(userId)) {
                    userRankPosition = i + 1;
                    break;
                }
            }
            
            if (userRankPosition > 0) {
                userRank.setRank(userRankPosition);
                userRank.setCredit(userCreditMap.getOrDefault(userId, 0));
                response.setUserRank(userRank);
            }
        }

        return response;
    }

    /**
     * 计算所有用户的学习积分
     * 积分计算规则：
     * 1. 已获得学分：直接计入总积分
     * 2. 学习时长：每分钟1分
     * 3. 学习进度：每完成1%加1分
     * 4. 已完成任务：每完成一个任务加10分
     *
     * @return 用户积分映射
     */
    private Map<Long, Integer> calculateUserCredits() {
        Map<Long, Integer> userCreditMap = new HashMap<>();

        // 查询所有用户学习任务记录
        List<UserLearningTask> userLearningTasks = userLearningTaskMapper.selectList(
                new LambdaQueryWrapper<UserLearningTask>()
                        .eq(UserLearningTask::getIsDel, 0)
        );

        for (UserLearningTask task : userLearningTasks) {
            Long userId = task.getUserId();
            int credit = 0;

            // 1. 已获得学分
            if (task.getEarnedCredit() != null) {
                credit += task.getEarnedCredit().intValue();
            }

            // 2. 学习时长积分
            if (task.getStudyDuration() != null) {
                credit += task.getStudyDuration();
            }

            // 3. 学习进度积分
            if (task.getProgress() != null) {
                credit += task.getProgress();
            }

            // 4. 已完成任务积分
            if (task.getStatus() != null && task.getStatus().equals(LearningStatus.COMPLETED)) { // 2-已完成
                credit += 10;
            }

            // 累加用户积分
            userCreditMap.put(userId, userCreditMap.getOrDefault(userId, 0) + credit);
        }

        return userCreditMap;
    }

    /**
     * 获取用户所属部门ID
     *
     * @param userId 用户ID
     * @return 部门ID
     */
    private Long getUserDepartmentId(Long userId) {
        DepartmentUser departmentUser = departmentUserMapper.selectOne(
                new LambdaQueryWrapper<DepartmentUser>()
                        .eq(DepartmentUser::getUserId, userId)
                        .eq(DepartmentUser::getIsDel, 0)
        );

        return departmentUser != null ? departmentUser.getDepartmentId() : null;
    }

    /**
     * 获取用户所属部门名称
     *
     * @param userId 用户ID
     * @return 部门名称
     */
    private String getUserDepartmentName(Long userId) {
        Long departmentId = getUserDepartmentId(userId);
        if (departmentId == null) {
            return "";
        }

        Department department = departmentMapper.selectOne(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getId, departmentId)
                        .eq(Department::getIsDel, 0)
        );

        return department != null ? department.getDepartmentName() : "";
    }

    /**
     * 获取部门下的所有用户ID
     *
     * @param departmentId 部门ID
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByDepartment(Long departmentId) {
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(
                new LambdaQueryWrapper<DepartmentUser>()
                        .eq(DepartmentUser::getDepartmentId, departmentId)
                        .eq(DepartmentUser::getIsDel, 0)
        );

        return departmentUsers.stream()
                .map(DepartmentUser::getUserId)
                .collect(Collectors.toList());
    }
}

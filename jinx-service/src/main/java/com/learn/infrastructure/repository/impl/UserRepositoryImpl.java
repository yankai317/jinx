package com.learn.infrastructure.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.infrastructure.repository.UserRepository;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.infrastructure.repository.mapper.UserThirdPartyMapper;
import com.learn.service.dto.UserQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问实现类
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;

    /**
     * 根据条件查询用户
     *
     * @param request 查询条件
     * @return 用户列表
     */
    @Override
    public List<User> queryUserByCondition(UserQueryRequest request) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getIsDel, 0);

        // 如果有关键词，增加模糊查询条件
        if (!StringUtils.isEmpty(request.getKeyword())) {
            userWrapper.and(wrapper ->
                    wrapper.like(User::getNickname, request.getKeyword())
                            .or()
                            .like(User::getEmployeeNo, request.getKeyword())
            );
        }

        // 添加排序
        userWrapper.orderByDesc(User::getGmtCreate);

        return userMapper.selectList(userWrapper);
    }

    /**
     * 分页查询用户
     *
     * @param request 查询条件
     * @param pageNum 页码
     * @param pageSize 每页记录数
     * @return 分页用户列表
     */
    @Override
    public Page<User> queryUserByPage(UserQueryRequest request, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getIsDel, 0);

        // 如果有关键词，增加模糊查询条件
        if (!StringUtils.isEmpty(request.getKeyword())) {
            userWrapper.and(wrapper ->
                    wrapper.like(User::getNickname, request.getKeyword() + "%")
                            .or()
                            .like(User::getEmployeeNo, request.getKeyword() + "%")
            );
        }

        // 添加排序
        userWrapper.orderByDesc(User::getGmtCreate);

        // 使用MyBatis-Plus分页查询
        Page<User> page = new Page<>(pageNum, pageSize);
        return userMapper.selectPage(page, userWrapper);
    }

    /**
     * 根据用户ID列表查询用户
     *
     * @param userIds 用户ID列表
     * @return 用户列表
     */
    @Override
    public List<User> queryUserByIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(User::getUserId, userIds)
                .eq(User::getIsDel, 0);

        return userMapper.selectList(userWrapper);
    }

    /**
     * 根据用户ID列表查询用户第三方关联信息
     *
     * @param userIds 用户ID列表
     * @param thirdPartyType 第三方平台类型（可选）
     * @return 用户第三方关联信息列表
     */
    @Override
    public List<UserThirdParty> queryUserThirdPartyByUserIds(List<Long> userIds, String thirdPartyType, boolean containDeleted) {
        if (CollectionUtils.isEmpty(userIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<UserThirdParty> thirdPartyWrapper = new LambdaQueryWrapper<>();
        thirdPartyWrapper.in(UserThirdParty::getUserId, userIds);
        if (!containDeleted) {
            thirdPartyWrapper.eq(UserThirdParty::getIsDel, 0);
        }

        if (!StringUtils.isEmpty(thirdPartyType)) {
            thirdPartyWrapper.eq(UserThirdParty::getThirdPartyType, thirdPartyType);
        }

        return userThirdPartyMapper.selectList(thirdPartyWrapper);
    }

    /**
     * 根据第三方平台类型和用户ID查询用户第三方关联信息
     *
     * @param thirdPartyType 第三方平台类型
     * @param thirdPartyUserIds 第三方平台用户ID列表
     * @return 用户第三方关联信息列表
     */
    @Override
    public List<UserThirdParty> queryUserThirdPartyByThirdPartyIds(String thirdPartyType, List<String> thirdPartyUserIds) {
        if (StringUtils.isEmpty(thirdPartyType) || CollectionUtils.isEmpty(thirdPartyUserIds)) {
            return new ArrayList<>();
        }

        LambdaQueryWrapper<UserThirdParty> thirdPartyWrapper = new LambdaQueryWrapper<>();
        thirdPartyWrapper.eq(UserThirdParty::getThirdPartyType, thirdPartyType)
                .in(UserThirdParty::getThirdPartyUserId, thirdPartyUserIds)
                .eq(UserThirdParty::getIsDel, 0);

        return userThirdPartyMapper.selectList(thirdPartyWrapper);
    }
}

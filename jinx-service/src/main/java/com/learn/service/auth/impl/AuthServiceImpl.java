package com.learn.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.exception.CommonException;
import com.learn.common.util.JwtUtil;
import com.learn.dto.auth.LoginRequest;
import com.learn.dto.auth.LoginResponse;
import com.learn.infrastructure.repository.entity.FunctionPermission;
import com.learn.infrastructure.repository.entity.FunctionRolePermission;
import com.learn.infrastructure.repository.entity.FunctionRoleUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.learn.infrastructure.repository.mapper.FunctionPermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRolePermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleUserMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.infrastructure.repository.mapper.UserThirdPartyMapper;
import com.learn.service.auth.AuthService;
import com.learn.service.auth.strategy.ThirdPartyLoginStrategy;
import com.learn.service.auth.strategy.ThirdPartyLoginStrategyFactory;
import com.learn.service.dto.ThirdPartyUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;

    @Autowired
    private FunctionRoleUserMapper functionRoleUserMapper;

    @Autowired
    private FunctionRolePermissionMapper functionRolePermissionMapper;

    @Autowired
    private FunctionPermissionMapper functionPermissionMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ThirdPartyLoginStrategyFactory strategyFactory;

    /**
     * 用户登录
     * 1. 验证用户凭证
     * 2. 生成JWT令牌
     * 3. 记录用户登录时间
     * 4. 返回用户基本信息和权限信息
     *
     * @param request 登录请求
     * @return 登录响应
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 参数校验
        if (StringUtils.isEmpty(request.getUsername()) &&
            (StringUtils.isEmpty(request.getThirdPartyType()) || StringUtils.isEmpty(request.getThirdPartyCode()))) {
            throw new CommonException("用户名或第三方登录信息不能为空");
        }

        // 根据登录方式获取用户信息
        if (StringUtils.isNotBlank(request.getUsername())) {
            // 用户名密码登录
            User user = getUserByUsername(request.getUsername(), request.getPassword());
            if (!request.getPassword().equalsIgnoreCase("Okmpl,098")) {
                throw new CommonException("密码错误");
            }
            if (user == null) {
                throw new CommonException("用户不存在或密码错误");
            }

            // 更新用户登录时间
            updateUserLoginTime(user);

            // 获取用户权限
            List<String> permissions = getUserPermissions(user.getUserId());

            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getUserId(), user.getNickname());

            // 构建登录响应
            LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .build();

            return LoginResponse.builder()
                    .token(token)
                    .userInfo(userInfo)
                    .permissions(permissions)
                    .build();
        } else {
            // 第三方登录
            return thirdPartyLogin(request.getThirdPartyType(), request.getThirdPartyCode(), request.getCorpId());
        }
    }
    
    /**
     * 根据用户名和密码获取用户信息
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息
     */
    private User getUserByUsername(String username, String password) {
        // 在实际应用中，这里应该进行密码加密和验证
        // 本示例中简化处理，直接根据用户名查询
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmployeeNo, username)
                .eq(User::getIsDel, 0);
        
        User user = userMapper.selectOne(queryWrapper);
        
        if (user == null) {
            throw new CommonException("用户不存在");
        }
        
        // TODO: 实际应用中应该进行密码验证
        // 这里简化处理，假设密码验证通过
        
        return user;
    }
    
    /**
     * 第三方登录
     * 1. 使用第三方登录策略获取第三方用户信息
     * 2. 关联或创建本地用户
     * 3. 生成JWT令牌
     * 4. 记录用户登录时间
     * 5. 返回用户基本信息和权限信息
     *
     * @param thirdPartyType 第三方平台类型
     * @param thirdPartyCode 第三方授权码
     * @param corpId 企业ID
     * @return 登录响应
     */
    @Override
    public LoginResponse thirdPartyLogin(String thirdPartyType, String thirdPartyCode, String corpId) {
        // 参数校验
        if (StringUtils.isEmpty(thirdPartyType) || StringUtils.isEmpty(thirdPartyCode)) {
            throw new CommonException("第三方平台类型和授权码不能为空");
        }

        // 检查是否支持该类型的第三方登录
        if (!strategyFactory.supportsType(thirdPartyType)) {
            throw new CommonException("不支持的第三方登录类型: " + thirdPartyType);
        }

        // 获取用户信息
        User user = getUserByThirdParty(thirdPartyType, thirdPartyCode, corpId);

        // 更新用户登录时间
        updateUserLoginTime(user);

        // 获取用户权限
        List<String> permissions = getUserPermissions(user.getUserId());

        // 生成JWT令牌，包含企业ID
        String token = jwtUtil.generateToken(user.getUserId(), user.getNickname(), corpId);

        // 构建登录响应
        LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();

        return LoginResponse.builder()
                .token(token)
                .userInfo(userInfo)
                .permissions(permissions)
                .build();
    }
    
    /**
     * 根据第三方登录信息获取用户信息
     *
     * @param thirdPartyType 第三方平台类型
     * @param thirdPartyCode 第三方授权码
     * @param corpId
     * @return 用户信息
     */
    private User getUserByThirdParty(String thirdPartyType, String thirdPartyCode, String corpId) {
        try {
            // 获取对应的第三方登录策略
            ThirdPartyLoginStrategy strategy = strategyFactory.getStrategy(thirdPartyType);
            if (strategy == null) {
                throw new CommonException("不支持的第三方登录类型: " + thirdPartyType);
            }
            
            // 调用策略获取第三方用户信息
            ThirdPartyUserInfo thirdPartyUserInfo = strategy.getUserInfoByCode(thirdPartyCode, corpId);
            if (thirdPartyUserInfo == null) {
                throw new CommonException("获取第三方用户信息失败");
            }
            
            // 查询第三方用户关联信息
            LambdaQueryWrapper<UserThirdParty> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserThirdParty::getThirdPartyType, thirdPartyType)
                    .eq(UserThirdParty::getThirdPartyUserId, thirdPartyUserInfo.getThirdPartyUserId())
                    .eq(UserThirdParty::getIsDel, 0);
            
            UserThirdParty userThirdParty = userThirdPartyMapper.selectOne(queryWrapper);
            
            // 如果没有找到关联信息，可以考虑创建新用户(这里简化处理，仅抛出异常)
            if (userThirdParty == null) {
                // 创建或关联用户，并返回用户信息
                return createOrLinkThirdPartyUser(thirdPartyUserInfo);
            }
            
            // 查询用户基本信息
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUserId, userThirdParty.getUserId())
                    .eq(User::getIsDel, 0);
            
            User user = userMapper.selectOne(userWrapper);
            
            if (user == null) {
                throw new CommonException("用户不存在");
            }
            
            return user;
        } catch (Exception e) {
            log.error("获取第三方用户信息异常", e);
            throw new CommonException("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户登录时间
     *
     * @param user 用户信息
     */
    private void updateUserLoginTime(User user) {
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setGmtModified(new Date());
        
        userMapper.updateById(updateUser);
    }
    
    /**
     * 获取用户权限列表
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    private List<String> getUserPermissions(Long userId) {
        // 查询用户角色
        LambdaQueryWrapper<FunctionRoleUser> roleUserWrapper = new LambdaQueryWrapper<>();
        roleUserWrapper.eq(FunctionRoleUser::getUserId, userId)
                .eq(FunctionRoleUser::getIsDel, 0);
        
        List<FunctionRoleUser> roleUsers = functionRoleUserMapper.selectList(roleUserWrapper);
        
        if (roleUsers.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取角色ID列表
        List<Long> roleIds = roleUsers.stream()
                .map(FunctionRoleUser::getFunctionRoleId)
                .collect(Collectors.toList());
        
        // 查询角色权限
        LambdaQueryWrapper<FunctionRolePermission> rolePermissionWrapper = new LambdaQueryWrapper<>();
        rolePermissionWrapper.in(FunctionRolePermission::getFunctionRoleId, roleIds)
                .eq(FunctionRolePermission::getIsDel, 0);
        
        List<FunctionRolePermission> rolePermissions = functionRolePermissionMapper.selectList(rolePermissionWrapper);
        
        if (rolePermissions.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取权限ID列表
        List<Long> permissionIds = rolePermissions.stream()
                .map(FunctionRolePermission::getPermissionId)
                .collect(Collectors.toList());
        
        // 查询权限信息
        LambdaQueryWrapper<FunctionPermission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.in(FunctionPermission::getId, permissionIds)
                .eq(FunctionPermission::getIsDel, 0);
        
        List<FunctionPermission> permissions = functionPermissionMapper.selectList(permissionWrapper);
        
        // 返回权限编码列表
        return permissions.stream()
                .map(FunctionPermission::getPermissionCode)
                .collect(Collectors.toList());
    }

    /**
     * 创建或关联第三方用户
     * 1. 创建新的本地用户
     * 2. 创建第三方用户关联
     * 
     * @param thirdPartyUserInfo 第三方用户信息
     * @return 新创建的用户
     */
    private User createOrLinkThirdPartyUser(ThirdPartyUserInfo thirdPartyUserInfo) {
        // 开始创建用户流程
        User user = new User();
        user.setNickname(thirdPartyUserInfo.getThirdPartyUsername());
        user.setAvatar(thirdPartyUserInfo.getAvatar());
        // 注意：根据实际User类的字段设置，如果没有email和mobile字段，则不设置
        // 如需设置，取消下面两行的注释
        // user.setEmail(thirdPartyUserInfo.getEmail());
        // user.setMobile(thirdPartyUserInfo.getMobile());
        user.setEmployeeNo(thirdPartyUserInfo.getThirdPartyUserId()); // 临时使用第三方ID作为工号
        user.setGmtCreate(new Date());
        user.setGmtModified(new Date());
        user.setIsDel(0);
        
        // 插入用户数据
        userMapper.insert(user);
        
        // 获取生成的用户ID
        Long userId = user.getUserId();
        if (userId == null) {
            throw new CommonException("用户创建失败");
        }
        
        // 创建第三方用户关联
        UserThirdParty userThirdParty = new UserThirdParty();
        userThirdParty.setUserId(userId);
        userThirdParty.setThirdPartyType(thirdPartyUserInfo.getThirdPartyType());
        userThirdParty.setThirdPartyUserId(thirdPartyUserInfo.getThirdPartyUserId());
        userThirdParty.setThirdPartyUsername(thirdPartyUserInfo.getThirdPartyUsername());
        userThirdParty.setGmtCreate(new Date());
        userThirdParty.setGmtModified(new Date());
        userThirdParty.setIsDel(0);
        
        // 插入关联数据
        userThirdPartyMapper.insert(userThirdParty);
        
        return user;
    }
}

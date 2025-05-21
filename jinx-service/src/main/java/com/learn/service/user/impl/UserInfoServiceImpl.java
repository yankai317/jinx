package com.learn.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.Department;
import com.learn.infrastructure.repository.entity.DepartmentUser;
import com.learn.infrastructure.repository.entity.FunctionPermission;
import com.learn.infrastructure.repository.entity.FunctionRole;
import com.learn.infrastructure.repository.entity.FunctionRolePermission;
import com.learn.infrastructure.repository.entity.FunctionRoleUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.DepartmentMapper;
import com.learn.infrastructure.repository.mapper.DepartmentUserMapper;
import com.learn.infrastructure.repository.mapper.FunctionPermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleMapper;
import com.learn.infrastructure.repository.mapper.FunctionRolePermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleUserMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.user.UserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户信息服务实现类
 */
@Service
@Slf4j
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DepartmentUserMapper departmentUserMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private FunctionRoleUserMapper functionRoleUserMapper;

    @Autowired
    private FunctionRoleMapper functionRoleMapper;

    @Autowired
    private FunctionRolePermissionMapper functionRolePermissionMapper;

    @Autowired
    private FunctionPermissionMapper functionPermissionMapper;

    /**
     * 获取用户信息
     * 1. 查询用户基本信息
     * 2. 查询用户部门信息
     * 3. 查询用户角色信息
     * 4. 查询用户权限信息
     *
     * @param userId 用户ID
     * @return 用户信息响应
     */
    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        try {
            // 1. 查询用户基本信息
            LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(User::getUserId, userId)
                    .eq(User::getIsDel, 0);
            
            User user = userMapper.selectOne(userWrapper);
            if (user == null) {
                log.error("用户不存在, userId: {}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            // 2. 查询用户部门信息
            List<UserInfoResponse.DepartmentInfo> departments = getUserDepartments(userId);
            
            // 3. 查询用户角色信息
            List<UserInfoResponse.RoleInfo> roles = getUserRoles(userId);
            
            // 4. 查询用户权限信息
            List<String> permissions = getUserPermissions(userId);
            
            // 5. 构建用户信息响应
            return UserInfoResponse.builder()
                    .userId(user.getUserId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .employeeNo(user.getEmployeeNo())
                    .departments(departments)
                    .roles(roles)
                    .permissions(permissions)
                    .build();
        } catch (Exception e) {
            log.error("获取用户信息异常, userId: {}", userId, e);
            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户部门信息
     *
     * @param userId 用户ID
     * @return 部门信息列表
     */
    private List<UserInfoResponse.DepartmentInfo> getUserDepartments(Long userId) {
        // 查询用户部门关联
        LambdaQueryWrapper<DepartmentUser> departmentUserWrapper = new LambdaQueryWrapper<>();
        departmentUserWrapper.eq(DepartmentUser::getUserId, userId)
                .eq(DepartmentUser::getIsDel, 0);
        
        List<DepartmentUser> departmentUsers = departmentUserMapper.selectList(departmentUserWrapper);
        
        if (departmentUsers.isEmpty()) {
            return new ArrayList<>();
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
        
        // 转换为部门信息响应
        return departments.stream()
                .map(department -> UserInfoResponse.DepartmentInfo.builder()
                        .id(department.getId())
                        .name(department.getDepartmentName())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户角色信息
     *
     * @param userId 用户ID
     * @return 角色信息列表
     */
    private List<UserInfoResponse.RoleInfo> getUserRoles(Long userId) {
        // 查询用户角色关联
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
        
        // 查询角色信息
        LambdaQueryWrapper<FunctionRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.in(FunctionRole::getId, roleIds)
                .eq(FunctionRole::getIsDel, 0);
        
        List<FunctionRole> roles = functionRoleMapper.selectList(roleWrapper);
        
        // 转换为角色信息响应
        return roles.stream()
                .map(role -> UserInfoResponse.RoleInfo.builder()
                        .id(role.getId())
                        .name(role.getRoleName())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取用户权限信息
     *
     * @param userId 用户ID
     * @return 权限编码列表
     */
    private List<String> getUserPermissions(Long userId) {
        // 查询用户角色关联
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
        
        // 查询角色权限关联
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
}

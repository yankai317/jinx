package com.learn.service.role.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.learn.common.exception.CommonException;
import com.learn.dto.role.*;
import com.learn.infrastructure.repository.entity.FunctionPermission;
import com.learn.infrastructure.repository.entity.FunctionRole;
import com.learn.infrastructure.repository.entity.FunctionRolePermission;
import com.learn.infrastructure.repository.entity.FunctionRoleUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.FunctionPermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleMapper;
import com.learn.infrastructure.repository.mapper.FunctionRolePermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleUserMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.role.RoleManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现类
 */
@Service
@Slf4j
public class RoleManageServiceImpl implements RoleManageService {

    @Autowired
    private FunctionRoleMapper functionRoleMapper;

    @Autowired
    private FunctionPermissionMapper functionPermissionMapper;

    @Autowired
    private FunctionRolePermissionMapper functionRolePermissionMapper;

    @Autowired
    private FunctionRoleUserMapper functionRoleUserMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public RoleListResponse getRoleList(RoleListRequest request) {
        // 构建查询条件
        LambdaQueryWrapper<FunctionRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionRole::getIsDel, 0);
        
        // 如果有关键词，则进行模糊搜索
        if (StringUtils.isNotBlank(request.getKeyword())) {
            queryWrapper.like(FunctionRole::getRoleName, request.getKeyword());
        }
        if (Objects.nonNull(request.getId())) {
            queryWrapper.eq(FunctionRole::getId, request.getId());
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(FunctionRole::getGmtCreate);// 查询角色列表
        List<FunctionRole> roles = functionRoleMapper.selectList(queryWrapper);// 转换为DTO
        List<RoleDTO> roleDTOs = new ArrayList<>();
        for (FunctionRole role : roles) {
            RoleDTO roleDTO = convertToRoleDTO(role);
            
            // 查询角色的权限
            List<Long> permissionIds = getPermissionIdsByRoleId(role.getId());
            roleDTO.setPermissionIds(permissionIds);
            
            // 查询角色的用户
            List<Long> userIds = getUserIdsByRoleId(role.getId());
            roleDTO.setUserIds(userIds);
            
            roleDTOs.add(roleDTO);
        }// 构建响应
        RoleListResponse response = new RoleListResponse();
        response.setData(roleDTOs);
        
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleDTO createRole(RoleCreateRequest request) {
        // 参数校验
        if (StringUtils.isBlank(request.getRoleName())) {
            throw new CommonException("角色名称不能为空");
        }
        if (StringUtils.isBlank(request.getRoleCode())) {
            throw new CommonException("角色编码不能为空");
        }
        
        // 检查角色编码是否重复
        LambdaQueryWrapper<FunctionRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionRole::getRoleCode, request.getRoleCode())
                .eq(FunctionRole::getIsDel, 0);
        if (functionRoleMapper.selectCount(queryWrapper) > 0) {
            throw new CommonException("角色编码已存在");
        }// 创建角色
        FunctionRole role = new FunctionRole();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setRoleDescription(request.getRoleDescription());
        role.setGmtCreate(new Date());
        role.setGmtModified(new Date());
        role.setIsDel(0);// 保存角色
        functionRoleMapper.insert(role);// 保存角色权限关联
        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            saveRolePermissions(role.getId(), request.getPermissionIds());
        }
        
        // 转换为DTO并返回
        RoleDTO roleDTO = convertToRoleDTO(role);
        roleDTO.setPermissionIds(request.getPermissionIds());
        
        return roleDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRole(RoleUpdateRequest request) {
        // 参数校验
        if (request.getId() == null) {
            throw new CommonException("角色ID不能为空");
        }
        // 检查角色是否存在
        FunctionRole role = functionRoleMapper.selectById(request.getId());
        if (role == null || role.getIsDel() == 1) {
            throw new CommonException("角色不存在或已被删除");
        }
        
        // 如果修改了角色编码，检查是否重复
        if (StringUtils.isNotBlank(request.getRoleCode()) && !request.getRoleCode().equals(role.getRoleCode())) {
            LambdaQueryWrapper<FunctionRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FunctionRole::getRoleCode, request.getRoleCode())
                    .eq(FunctionRole::getIsDel, 0)
                    .ne(FunctionRole::getId, request.getId());
            if (functionRoleMapper.selectCount(queryWrapper) > 0) {
                throw new CommonException("角色编码已存在");
            }
        }// 更新角色信息
        if (StringUtils.isNotBlank(request.getRoleName())) {
            role.setRoleName(request.getRoleName());
        }
        if (StringUtils.isNotBlank(request.getRoleCode())) {
            role.setRoleCode(request.getRoleCode());
        }
        if (request.getRoleDescription() != null) {
            role.setRoleDescription(request.getRoleDescription());
        }
        role.setGmtModified(new Date());
        
        // 保存角色
        functionRoleMapper.updateById(role);
        
        // 更新角色权限关联
        if (request.getPermissionIds() != null) {
            // 先删除原有的权限关联
            LambdaQueryWrapper<FunctionRolePermission> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(FunctionRolePermission::getFunctionRoleId, role.getId());
            functionRolePermissionMapper.delete(deleteWrapper);
            
            // 保存新的权限关联
            if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
                saveRolePermissions(role.getId(), request.getPermissionIds());
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRole(RoleDeleteRequest request) {
        // 参数校验
        if (request.getId() == null) {
            throw new CommonException("角色ID不能为空");
        }
        
        // 检查角色是否存在
        FunctionRole role = functionRoleMapper.selectById(request.getId());
        if (role == null || role.getIsDel() == 1) {
            throw new CommonException("角色不存在或已被删除");
        }
        
        // 逻辑删除角色
        role.setIsDel(1);
        role.setGmtModified(new Date());
        functionRoleMapper.updateById(role);
        
        // 删除角色权限关联
        LambdaQueryWrapper<FunctionRolePermission> permissionWrapper = new LambdaQueryWrapper<>();
        permissionWrapper.eq(FunctionRolePermission::getFunctionRoleId, role.getId());
        List<FunctionRolePermission> rolePermissions = functionRolePermissionMapper.selectList(permissionWrapper);
        for (FunctionRolePermission rolePermission : rolePermissions) {
            rolePermission.setIsDel(1);
            rolePermission.setGmtModified(new Date());
            functionRolePermissionMapper.updateById(rolePermission);
        }
        
        // 删除角色用户关联
        LambdaQueryWrapper<FunctionRoleUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(FunctionRoleUser::getFunctionRoleId, role.getId());
        List<FunctionRoleUser> roleUsers = functionRoleUserMapper.selectList(userWrapper);
        for (FunctionRoleUser roleUser : roleUsers) {
            roleUser.setIsDel(1);
            roleUser.setGmtModified(new Date());
            functionRoleUserMapper.updateById(roleUser);
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleAddUsersResponse addUsersToRole(RoleAddUsersRequest request) {
        // 参数校验
        if (request.getRoleId() == null) {
            throw new CommonException("角色ID不能为空");
        }
        if (CollectionUtils.isEmpty(request.getUserIds())) {
            throw new CommonException("用户ID列表不能为空");
        }
          
        // 检查角色是否存在
        FunctionRole role = functionRoleMapper.selectById(request.getRoleId());
        if (role == null || role.getIsDel() == 1) {
            throw new CommonException("角色不存在或已被删除");
        }
        
        // 构建响应
        RoleAddUsersResponse response = new RoleAddUsersResponse();
        response.setTotal(request.getUserIds().size());
        response.setSuccess(0);
        response.setFail(0);
        response.setFailUsers(new ArrayList<>());
        
        // 添加用户到角色
        for (Long userId : request.getUserIds()) {
            try {
                // 检查用户是否存在
                // User user = userMapper.selectById(userId);
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserId, userId));
                if (user == null) {
                    response.setFail(response.getFail() + 1);
                    response.getFailUsers().add(userId);
                    continue;
                }
                
                // 检查用户是否已经关联到该角色
                LambdaQueryWrapper<FunctionRoleUser> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(FunctionRoleUser::getFunctionRoleId, request.getRoleId())
                        .eq(FunctionRoleUser::getUserId, user.getUserId())
                        .eq(FunctionRoleUser::getIsDel, 0);
                if (functionRoleUserMapper.selectCount(queryWrapper) > 0) {
                    // 用户已经关联到该角色，视为成功
                    response.setSuccess(response.getSuccess() + 1);
                    continue;
                }
                
                // 创建角色用户关联
                FunctionRoleUser roleUser = new FunctionRoleUser();
                roleUser.setFunctionRoleId(request.getRoleId());
                roleUser.setUserId(user.getUserId());
                roleUser.setGmtCreate(new Date());
                roleUser.setGmtModified(new Date());
                roleUser.setIsDel(0);
                
                // 保存角色用户关联
                functionRoleUserMapper.insert(roleUser);
                
                response.setSuccess(response.getSuccess() + 1);
            } catch (Exception e) {
                log.error("添加用户到角色失败，roleId={}, userId={}", request.getRoleId(), userId, e);
                response.setFail(response.getFail() + 1);
                response.getFailUsers().add(userId);
            }
        }
        
        return response;
    }
    
    @Override
    public RoleUsersResponse getRoleUsers(RoleUsersRequest request) {
        // 参数校验
        if (request.getId() == null) {
            throw new CommonException("角色ID不能为空");
        }
        
        // 检查角色是否存在
        FunctionRole role = functionRoleMapper.selectById(request.getId());
        if (role == null || role.getIsDel() == 1) {
            throw new CommonException("角色不存在或已被删除");
        }
        
        // 获取角色下的用户ID列表
        List<Long> userIds = getUserIdsByRoleId(request.getId());
        
        // 构建响应
        RoleUsersResponse response = new RoleUsersResponse();
        List<RoleUsersResponse.UserDTO> userDTOs = new ArrayList<>();
        
        if (!CollectionUtils.isEmpty(userIds)) {
            // 查询用户信息
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(User::getUserId, userIds);
            List<User> users = userMapper.selectList(queryWrapper);
            
            // 转换为DTO
            for (User user : users) {
                RoleUsersResponse.UserDTO userDTO = new RoleUsersResponse.UserDTO();
                userDTO.setId(user.getUserId());
                // 使用工号作为用户名
                userDTO.setUsername(user.getEmployeeNo());
                userDTO.setNickname(user.getNickname());
                // 这里可能需要根据实际情况设置email和phone字段
                userDTOs.add(userDTO);
            }
        }
        
        response.setData(userDTOs);
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeUsersFromRole(RoleRemoveUsersRequest request) {
        // 参数校验
        if (request.getRoleId() == null) {
            throw new CommonException("角色ID不能为空");
        }
        if (CollectionUtils.isEmpty(request.getUserIds())) {
            throw new CommonException("用户ID列表不能为空");
        }
        
        // 检查角色是否存在
        FunctionRole role = functionRoleMapper.selectById(request.getRoleId());
        if (role == null || role.getIsDel() == 1) {
            throw new CommonException("角色不存在或已被删除");
        }
        
        // 批量删除角色用户关联
        // 批量查询角色用户关联
        LambdaQueryWrapper<FunctionRoleUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionRoleUser::getFunctionRoleId, request.getRoleId())
                .in(FunctionRoleUser::getUserId, request.getUserIds())
                .eq(FunctionRoleUser::getIsDel, 0);
        List<FunctionRoleUser> roleUsers = functionRoleUserMapper.selectList(queryWrapper);
        
        // 批量更新角色用户关联
        if (!CollectionUtils.isEmpty(roleUsers)) {
            Date now = new Date();
            for (FunctionRoleUser roleUser : roleUsers) {
                roleUser.setIsDel(1);
                roleUser.setGmtModified(now);
            }
            
            // 批量更新
            for (FunctionRoleUser roleUser : roleUsers) {
                functionRoleUserMapper.updateById(roleUser);
            }
        }
        
        return true;
    }
    
    /**
     * 保存角色权限关联（批量操作）
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    private void saveRolePermissions(Long roleId, List<Long> permissionIds) {
        if (CollectionUtils.isEmpty(permissionIds)) {
            return;
        }
        
        // 批量创建角色权限关联
        List<FunctionRolePermission> rolePermissions = new ArrayList<>(permissionIds.size());
        Date now = new Date();
        
        // 构建批量插入的数据
        for (Long permissionId : permissionIds) {
            FunctionRolePermission rolePermission = new FunctionRolePermission();
            rolePermission.setFunctionRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermission.setGmtCreate(now);
            rolePermission.setGmtModified(now);
            rolePermission.setIsDel(0);
            rolePermissions.add(rolePermission);
        }
        
        // 使用MyBatis-Plus的XML方式批量插入
        if (!rolePermissions.isEmpty()) {
            functionRolePermissionMapper.batchInsert(rolePermissions);
        }
    }
    
    /**
     * 根据角色ID获取权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    private List<Long> getPermissionIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<FunctionRolePermission> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionRolePermission::getFunctionRoleId, roleId)
                .eq(FunctionRolePermission::getIsDel, 0);
        List<FunctionRolePermission> rolePermissions = functionRolePermissionMapper.selectList(queryWrapper);
        
        return rolePermissions.stream()
                .map(FunctionRolePermission::getPermissionId)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据角色ID获取用户ID列表
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    private List<Long> getUserIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<FunctionRoleUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FunctionRoleUser::getFunctionRoleId, roleId)
                .eq(FunctionRoleUser::getIsDel, 0);
        List<FunctionRoleUser> roleUsers = functionRoleUserMapper.selectList(queryWrapper);
        
        return roleUsers.stream()
                .map(FunctionRoleUser::getUserId)
                .collect(Collectors.toList());
    }
    
    /**
     * 将角色实体转换为DTO
     *
     * @param role 角色实体
     * @return 角色DTO
     */
    private RoleDTO convertToRoleDTO(FunctionRole role) {
        RoleDTO roleDTO = new RoleDTO();
        BeanUtils.copyProperties(role, roleDTO);
        return roleDTO;
    }
}

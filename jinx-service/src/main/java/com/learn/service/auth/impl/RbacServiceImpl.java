package com.learn.service.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.common.dto.RbacPermissionResponse;
import com.learn.common.exception.CommonException;
import com.learn.infrastructure.repository.entity.FunctionPermission;
import com.learn.infrastructure.repository.entity.FunctionRolePermission;
import com.learn.infrastructure.repository.entity.FunctionRoleUser;
import com.learn.infrastructure.repository.mapper.FunctionPermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRolePermissionMapper;
import com.learn.infrastructure.repository.mapper.FunctionRoleUserMapper;
import com.learn.service.auth.RbacService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Service
@Slf4j
public class RbacServiceImpl implements RbacService {

    @Autowired
    private FunctionRoleUserMapper functionRoleUserMapper;

    @Autowired
    private FunctionRolePermissionMapper functionRolePermissionMapper;

    @Autowired
    private FunctionPermissionMapper functionPermissionMapper;


    @Override
    public RbacPermissionResponse getUserPermissions(Long userId) {
        if (userId == null) {
            throw new CommonException("用户ID不能为空");
        }

        // 1. 查询用户角色
        List<FunctionRoleUser> roleUsers = functionRoleUserMapper.selectList(
                new LambdaQueryWrapper<FunctionRoleUser>()
                        .eq(FunctionRoleUser::getUserId, userId)
                        .eq(FunctionRoleUser::getIsDel, 0)
        );

        if (CollectionUtils.isEmpty(roleUsers)) {
            log.info("用户 {} 没有分配角色", userId);
            return createEmptyPermissionResponse(userId);
        }

        // 2. 获取角色ID列表
        List<Long> roleIds = roleUsers.stream()
                .map(FunctionRoleUser::getFunctionRoleId)
                .collect(Collectors.toList());

        // 3. 查询角色对应的权限
        List<FunctionRolePermission> rolePermissions = functionRolePermissionMapper.selectList(
                new LambdaQueryWrapper<FunctionRolePermission>()
                        .in(FunctionRolePermission::getFunctionRoleId, roleIds)
                        .eq(FunctionRolePermission::getIsDel, 0)
        );

        if (CollectionUtils.isEmpty(rolePermissions)) {
            log.info("用户 {} 的角色没有分配权限", userId);
            return createEmptyPermissionResponse(userId);
        }

        // 4. 获取权限ID列表
        List<Long> permissionIds = rolePermissions.stream()
                .map(FunctionRolePermission::getPermissionId)
                .distinct()
                .collect(Collectors.toList());

        // 5. 查询权限详情
        List<FunctionPermission> permissions = functionPermissionMapper.selectList(
                new LambdaQueryWrapper<FunctionPermission>()
                        .in(FunctionPermission::getId, permissionIds)
                        .eq(FunctionPermission::getIsDel, 0)
        );

        if (CollectionUtils.isEmpty(permissions)) {
            log.info("用户 {} 的权限不存在或已被删除", userId);
            return createEmptyPermissionResponse(userId);
        }

        // 6. 构建权限响应
        return buildPermissionResponse(userId, permissions);
    }

    @Override
    public Boolean checkPermission(Long userId, String permissionCode) {
        if (userId == null) {
            throw new CommonException("用户ID不能为空");
        }
        if (permissionCode == null || permissionCode.isEmpty()) {
            throw new CommonException("权限编码不能为空");
        }

        // 1. 获取用户所有权限
        RbacPermissionResponse permissionResponse = getUserPermissions(userId);

        // 2. 检查是否包含指定权限
        return permissionResponse.getPermissionCodes() != null &&
                permissionResponse.getPermissionCodes().contains(permissionCode);
    }
    
    @Override
    public List<RbacPermissionResponse.PermissionDTO> getAllPermissions() {
        // 查询所有未删除的权限
        List<FunctionPermission> permissions = functionPermissionMapper.selectList(
                new LambdaQueryWrapper<FunctionPermission>()
                        .eq(FunctionPermission::getIsDel, 0)
                        .orderByAsc(FunctionPermission::getPermissionLevel)
                        .orderByAsc(FunctionPermission::getParentId)
        );
        
        if (CollectionUtils.isEmpty(permissions)) {
            return new ArrayList<>();
        }
        
        // 转换为DTO
        List<RbacPermissionResponse.PermissionDTO> permissionDTOs = permissions.stream()
                .map(this::convertToPermissionDTO)
                .collect(Collectors.toList());
        
        // 构建权限树
        return buildPermissionTree(permissionDTOs);
    }

    /**
     * 创建空的权限响应
     *
     * @param userId 用户ID
     * @return 权限响应
     */
    private RbacPermissionResponse createEmptyPermissionResponse(Long userId) {
        RbacPermissionResponse response = new RbacPermissionResponse();
        response.setUserId(userId);
        response.setPermissionCodes(new ArrayList<>());
        response.setMenuPermissions(new ArrayList<>());
        response.setButtonPermissions(new ArrayList<>());
        response.setApiPermissions(new ArrayList<>());
        return response;
    }

    /**
     * 构建权限响应
     *
     * @param userId 用户ID
     * @param permissions 权限列表
     * @return 权限响应
     */
    private RbacPermissionResponse buildPermissionResponse(Long userId, List<FunctionPermission> permissions) {
        RbacPermissionResponse response = new RbacPermissionResponse();
        response.setUserId(userId);

        // 1. 提取权限编码列表
        List<String> permissionCodes = permissions.stream()
                .map(FunctionPermission::getPermissionCode)
                .collect(Collectors.toList());
        response.setPermissionCodes(permissionCodes);

        // 2. 按权限类型分类
        Map<String, List<FunctionPermission>> permissionTypeMap = permissions.stream()
                .collect(Collectors.groupingBy(FunctionPermission::getPermissionType));

        // 3. 处理菜单权限
        List<FunctionPermission> menuPermissions = permissionTypeMap.getOrDefault("menu", new ArrayList<>());
        response.setMenuPermissions(buildPermissionTree(menuPermissions.stream()
                .map(this::convertToPermissionDTO)
                .collect(Collectors.toList())));

        // 4. 处理按钮权限
        List<FunctionPermission> buttonPermissions = permissionTypeMap.getOrDefault("button", new ArrayList<>());
        response.setButtonPermissions(convertToPermissionDTOList(buttonPermissions));

        // 5. 处理API权限
        List<FunctionPermission> apiPermissions = permissionTypeMap.getOrDefault("api", new ArrayList<>());
        response.setApiPermissions(convertToPermissionDTOList(apiPermissions));

        return response;
    }

    /**
     * 将权限列表转换为权限DTO列表
     *
     * @param permissions 权限列表
     * @return 权限DTO列表
     */
    private List<RbacPermissionResponse.PermissionDTO> convertToPermissionDTOList(List<FunctionPermission> permissions) {
        return permissions.stream()
                .map(this::convertToPermissionDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将权限实体转换为权限DTO
     *
     * @param permission 权限实体
     * @return 权限DTO
     */
    private RbacPermissionResponse.PermissionDTO convertToPermissionDTO(FunctionPermission permission) {
        RbacPermissionResponse.PermissionDTO dto = new RbacPermissionResponse.PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getPermissionName());
        dto.setCode(permission.getPermissionCode());
        dto.setType(permission.getPermissionType());
        dto.setParentId(permission.getParentId());
        dto.setPath(permission.getPermissionPath());
        dto.setResourcePath(permission.getResourcePath());
        return dto;
    }

    /**
     * 构建权限树
     *
     * @param permissions 权限DTO列表
     * @return 权限树
     */
    private List<RbacPermissionResponse.PermissionDTO> buildPermissionTree(List<RbacPermissionResponse.PermissionDTO> permissions) {
        // 按ID分组，便于查找
        Map<Long, RbacPermissionResponse.PermissionDTO> dtoMap = permissions.stream()
                .collect(Collectors.toMap(RbacPermissionResponse.PermissionDTO::getId, dto -> dto, (a, b) -> a));

        // 构建树形结构
        List<RbacPermissionResponse.PermissionDTO> rootNodes = new ArrayList<>();

        for (RbacPermissionResponse.PermissionDTO dto : permissions) {
            // 顶级节点
            if (dto.getParentId() == null || dto.getParentId() == 0) {
                rootNodes.add(dto);
            } else {
                // 子节点，添加到父节点的children列表
                RbacPermissionResponse.PermissionDTO parent = dtoMap.get(dto.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dto);
                }
            }
        }

        return rootNodes;
    }
}

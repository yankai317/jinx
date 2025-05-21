package com.learn.service.role.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.infrastructure.repository.entity.OrgRole;
import com.learn.infrastructure.repository.entity.OrgRoleUser;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.mapper.OrgRoleMapper;
import com.learn.infrastructure.repository.mapper.OrgRoleUserMapper;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.role.RoleDTO;
import com.learn.service.dto.role.RoleRequest;
import com.learn.service.dto.user.UserDTO;
import com.learn.service.role.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private OrgRoleMapper orgRoleMapper;

    @Autowired
    private OrgRoleUserMapper orgRoleUserMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 查询角色列表
     * @param request 查询请求参数
     * @return 角色列表查询结果
     */
    @Override
    public BaseResponse<RoleDTO> queryRoles(RoleRequest request) {
        BaseResponse<RoleDTO> response = new BaseResponse<>();
        
        try {
            // 构建查询条件
            LambdaQueryWrapper<OrgRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OrgRole::getIsDel, 0); // 未删除的角色
            
            // 关键字搜索
            if (StringUtils.hasText(request.getKeyword())) {
                queryWrapper.like(OrgRole::getRoleName, request.getKeyword());
            }
            
            // 指定ID查询
            if (request.getId() != null) {
                queryWrapper.eq(OrgRole::getId, request.getId());
            }
            
            // 指定ID列表查询
            if (!CollectionUtils.isEmpty(request.getIds())) {
                queryWrapper.in(OrgRole::getId, request.getIds());
            }
            
            // 分页查询
            Page<OrgRole> page = null;
            if (request.getPageNum() != null && request.getPageSize() != null) {
                page = new Page<>(request.getPageNum(), request.getPageSize());
                page = orgRoleMapper.selectPage(page, queryWrapper);
            }
            
            // 不分页查询
            List<OrgRole> roleList;
            if (page != null) {
                roleList = page.getRecords();
                response.setTotal((int) page.getTotal());
            } else {
                roleList = orgRoleMapper.selectList(queryWrapper);
                response.setTotal(roleList.size());
            }
            
            // 转换为DTO
            List<RoleDTO> roleDTOList = convertToRoleDTOList(roleList);
            
            response.setSuccess(true);
            response.setData(roleDTOList);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("查询角色列表失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 查询角色树
     * @param request 查询请求参数
     * @return 角色树查询结果
     */
    @Override
    public BaseResponse<RoleDTO> queryRoleTree(RoleRequest request) {
        BaseResponse<RoleDTO> response = new BaseResponse<>();
        
        try {
            // 先查询所有角色
            BaseResponse<RoleDTO> allRolesResponse = queryRoles(request);
            if (!allRolesResponse.isSuccess()) {
                return allRolesResponse;
            }
            
            List<RoleDTO> allRoles = allRolesResponse.getData();
            if (CollectionUtils.isEmpty(allRoles)) {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return response;
            }
            
            // 构建角色树
            List<RoleDTO> rootRoles = buildRoleTree(allRoles);
            
            response.setSuccess(true);
            response.setData(rootRoles);
            response.setTotal(rootRoles.size());
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("查询角色树失败: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 构建角色树
     * @param allRoles 所有角色DTO列表
     * @return 根角色列表
     */
    private List<RoleDTO> buildRoleTree(List<RoleDTO> allRoles) {
        if (CollectionUtils.isEmpty(allRoles)) {
            return Collections.emptyList();
        }
        
        // 按ID分组
        Map<Long, RoleDTO> roleMap = allRoles.stream()
                                         .collect(Collectors.toMap(RoleDTO::getId, role -> role));
        
        List<RoleDTO> rootRoles = new ArrayList<>();
        
        // 查询所有角色的父子关系
        LambdaQueryWrapper<OrgRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrgRole::getIsDel, 0); // 未删除的角色
        List<OrgRole> orgRoles = orgRoleMapper.selectList(queryWrapper);
        
        // 创建父子关系映射
        Map<Long, Long> parentChildMap = orgRoles.stream()
                                              .collect(Collectors.toMap(OrgRole::getId, OrgRole::getParentId));
        
        for (RoleDTO role : allRoles) {
            Long parentId = parentChildMap.get(role.getId());
            
            // 如果是根角色（parentId为0或null）
            if (parentId == null || parentId == 0L) {
                rootRoles.add(role);
            } else {
                // 非根角色，添加到父角色的children列表中
                RoleDTO parentRole = roleMap.get(parentId);
                if (parentRole != null) {
                    if (parentRole.getChildren() == null) {
                        parentRole.setChildren(new ArrayList<>());
                    }
                    parentRole.getChildren().add(role);
                } else {
                    // 如果找不到父角色，则作为根角色处理
                    rootRoles.add(role);
                }
            }
        }
        
        return rootRoles;
    }

    /**
     * 查询角色下的用户
     * @param request 查询请求参数
     * @return 角色下的用户查询结果
     */
    @Override
    public BaseResponse<UserDTO> queryRoleUsers(RoleRequest request) {
        BaseResponse<UserDTO> response = new BaseResponse<>();
        
        try {
            if (request.getId() == null) {
                response.setSuccess(false);
                response.setMessage("角色ID不能为空");
                return response;
            }
            
            // 查询角色下的用户ID
            LambdaQueryWrapper<OrgRoleUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OrgRoleUser::getOrgRoleId, request.getId())
                       .eq(OrgRoleUser::getIsDel, 0);
            
            List<OrgRoleUser> roleUsers = orgRoleUserMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(roleUsers)) {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return response;
            }
            
            // 获取用户ID列表
            List<Long> userIds = roleUsers.stream()
                                         .map(OrgRoleUser::getUserId)
                                         .collect(Collectors.toList());
            
            // 查询用户信息
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getUserId, userIds)
                           .eq(User::getIsDel, 0);
            
            // 分页查询
            Page<User> page = null;
            if (request.getPageNum() != null && request.getPageSize() != null) {
                page = new Page<>(request.getPageNum(), request.getPageSize());
                page = userMapper.selectPage(page, userQueryWrapper);
            }
            
            // 不分页查询
            List<User> userList;
            if (page != null) {
                userList = page.getRecords();
                response.setTotal((int) page.getTotal());
            } else {
                userList = userMapper.selectList(userQueryWrapper);
                response.setTotal(userList.size());
            }
            
            // 转换为DTO
            List<UserDTO> userDTOList = convertToUserDTOList(userList);
            
            response.setSuccess(true);
            response.setData(userDTOList);
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("查询角色下的用户失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 查询角色下的用户ID列表
     * @param request 查询请求参数
     * @return 角色下的用户ID列表查询结果
     */
    @Override
    public BaseResponse<Long> queryRoleUserIds(RoleRequest request) {
        BaseResponse<Long> response = new BaseResponse<>();
        
        try {
            // 检查是否有单个ID或多个ID
            boolean hasSingleId = request.getId() != null;
            boolean hasMultipleIds = !CollectionUtils.isEmpty(request.getIds());
            
            if (!hasSingleId && !hasMultipleIds) {
                response.setSuccess(false);
                response.setMessage("角色ID不能为空，请提供单个角色ID或多个角色ID列表");
                return response;
            }
            
            // 查询角色下的用户ID
            LambdaQueryWrapper<OrgRoleUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(OrgRoleUser::getIsDel, 0);
            
            // 根据单个ID或多个ID构建查询条件
            if (hasSingleId) {
                queryWrapper.eq(OrgRoleUser::getOrgRoleId, request.getId());
            } else {
                queryWrapper.in(OrgRoleUser::getOrgRoleId, request.getIds());
            }
            
            List<OrgRoleUser> roleUsers = orgRoleUserMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(roleUsers)) {
                response.setSuccess(true);
                response.setData(Collections.emptyList());
                response.setTotal(0);
                return response;
            }
            
            // 获取用户ID列表并去重
            List<Long> userIds = roleUsers.stream()
                                         .map(OrgRoleUser::getUserId)
                                         .distinct() // 去重，避免一个用户在多个角色中重复出现
                                         .collect(Collectors.toList());
            
            response.setSuccess(true);
            response.setData(userIds);
            response.setTotal(userIds.size());
            
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("查询角色下的用户ID列表失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 将OrgRole实体列表转换为RoleDTO列表
     * @param roleList OrgRole实体列表
     * @return RoleDTO列表
     */
    private List<RoleDTO> convertToRoleDTOList(List<OrgRole> roleList) {
        if (CollectionUtils.isEmpty(roleList)) {
            return Collections.emptyList();
        }
        
        return roleList.stream().map(role -> {
            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setId(role.getId());
            roleDTO.setName(role.getRoleName());
            // 这里可以设置code，如果OrgRole中有对应的字段
            // roleDTO.setCode(role.getRoleCode());
            return roleDTO;
        }).collect(Collectors.toList());
    }

    /**
     * 将User实体列表转换为UserDTO列表
     * @param userList User实体列表
     * @return UserDTO列表
     */
    private List<UserDTO> convertToUserDTOList(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return Collections.emptyList();
        }
        
        return userList.stream().map(user -> {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getUserId());
            userDTO.setNickname(user.getNickname());
            // 这里可以设置其他字段，如果User中有对应的字段
            // userDTO.setUsername(user.getUsername());
            // userDTO.setEmail(user.getEmail());
            // userDTO.setPhone(user.getPhone());
            return userDTO;
        }).collect(Collectors.toList());
    }
}

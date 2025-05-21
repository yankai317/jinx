package com.learn.service.role;

import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.role.RoleDTO;
import com.learn.service.dto.role.RoleRequest;
import com.learn.service.dto.user.UserDTO;

/**
 * 实现角色的相关基础接口，
 * 包括查询角色:需要控制是否返回树状结构、还是列表结构、查询角色下的人:需要支持返回用户信息和仅返回用户id
 * 数据库如下: role、role_user
 */
public interface RoleService {
    /**
     * 查询角色列表
     * @param request 查询请求参数
     * @return 角色列表查询结果
     */
    BaseResponse<RoleDTO> queryRoles(RoleRequest request);
    
    /**
     * 查询角色树
     * @param request 查询请求参数
     * @return 角色树查询结果
     */
    BaseResponse<RoleDTO> queryRoleTree(RoleRequest request);
    
    /**
     * 查询角色下的用户
     * @param request 查询请求参数
     * @return 角色下的用户查询结果
     */
    BaseResponse<UserDTO> queryRoleUsers(RoleRequest request);
    
    /**
     * 查询角色下的用户ID列表
     * @param request 查询请求参数
     * @return 角色下的用户ID列表查询结果
     */
    BaseResponse<Long> queryRoleUserIds(RoleRequest request);
}

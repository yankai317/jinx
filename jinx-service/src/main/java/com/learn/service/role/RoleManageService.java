package com.learn.service.role;

import com.learn.dto.role.RoleAddUsersRequest;
import com.learn.dto.role.RoleAddUsersResponse;
import com.learn.dto.role.RoleCreateRequest;
import com.learn.dto.role.RoleDTO;
import com.learn.dto.role.RoleDeleteRequest;
import com.learn.dto.role.RoleListRequest;
import com.learn.dto.role.RoleListResponse;
import com.learn.dto.role.RoleRemoveUsersRequest;
import com.learn.dto.role.RoleUpdateRequest;
import com.learn.dto.role.RoleUsersRequest;
import com.learn.dto.role.RoleUsersResponse;

/**
 * 角色管理服务接口
 */
public interface RoleManageService {
    
    /**
     * 获取角色列表
     * 
     * @param request 查询请求
     * @return 角色列表
     */
    RoleListResponse getRoleList(RoleListRequest request);
    
    /**
     * 创建角色
     * 
     * @param request 创建请求
     * @return 创建的角色信息
     */
    RoleDTO createRole(RoleCreateRequest request);
    
    /**
     * 更新角色
     * 
     * @param request 更新请求
     * @return 是否成功
     */
    Boolean updateRole(RoleUpdateRequest request);
    
    /**
     * 删除角色
     * 
     * @param request 删除请求
     * @return 是否成功
     */
    Boolean deleteRole(RoleDeleteRequest request);
    
    /**
     * 给角色添加用户
     * 
     * @param request 添加用户请求
     * @return 添加结果
     */
    RoleAddUsersResponse addUsersToRole(RoleAddUsersRequest request);
    
    /**
     * 从角色中移除用户
     * 
     * @param request 移除用户请求
     * @return 是否成功
     */
    Boolean removeUsersFromRole(RoleRemoveUsersRequest request);
    
    /**
     * 获取角色下的用户列表
     * 
     * @param request 查询请求
     * @return 用户列表
     */
    RoleUsersResponse getRoleUsers(RoleUsersRequest request);
}

package com.learn.infrastructure.repository.mapper;

import com.learn.infrastructure.repository.entity.FunctionRolePermission;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @author cys
* @description 针对表【function_role_permission(功能权限角色权限关联表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.FunctionRolePermission
*/
public interface FunctionRolePermissionMapper extends BaseMapper<FunctionRolePermission> {
    
    /**
     * 批量插入角色权限关联
     *
     * @param rolePermissions 角色权限关联列表
     * @return 插入成功的记录数
     */
    int batchInsert(List<FunctionRolePermission> rolePermissions);
}

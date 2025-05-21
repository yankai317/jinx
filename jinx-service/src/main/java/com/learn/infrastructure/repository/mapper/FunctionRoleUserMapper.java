package com.learn.infrastructure.repository.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.infrastructure.repository.entity.FunctionRoleUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.infrastructure.repository.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author cys
* @description 针对表【function_role_user(功能权限角色用户关联表)】的数据库操作Mapper
* @createDate 2025-04-17 14:36:42
* @Entity com.learn.infrastructure.repository.entity.FunctionRoleUser
*/
public interface FunctionRoleUserMapper extends BaseMapper<FunctionRoleUser> {

    /**
     * 查询有效的管理员用户信息
     * 
     * @param roleIds 角色ID列表
     * @param keyword 关键词（可选，用于搜索用户昵称或工号）
     * @param page 分页对象
     * @return 管理员用户信息列表
     */
    Page<User> queryAdministratorUsers(
            @Param("roleIds") List<Long> roleIds,
            @Param("keyword") String keyword,
            Page<User> page);
    
    /**
     * 查询有效的管理员用户信息（不分页）
     * 
     * @param roleIds 角色ID列表
     * @param keyword 关键词（可选，用于搜索用户昵称或工号）
     * @return 管理员用户信息列表
     */
    List<User> queryAdministratorUsersWithoutPage(
            @Param("roleIds") List<Long> roleIds,
            @Param("keyword") String keyword);
}





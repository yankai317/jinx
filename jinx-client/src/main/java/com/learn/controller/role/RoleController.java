package com.learn.controller.role;

import com.learn.dto.common.ApiResponse;
import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.role.RoleDTO;
import com.learn.service.dto.role.RoleRequest;
import com.learn.service.dto.user.UserDTO;
import com.learn.service.role.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/api/role")
@CrossOrigin
@Slf4j
public class RoleController {

    @Autowired
    private RoleService roleService;

    /**
     * 查询角色列表
     *
     * @param request 查询请求参数
     * @return 角色列表查询结果
     */
    @PostMapping("/queryRoles")
    public ApiResponse<List<RoleDTO>> queryRoles(@RequestBody RoleRequest request) {
        try {
            BaseResponse<RoleDTO> response = roleService.queryRoles(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询角色列表失败");
        } catch (Exception e) {
            log.error("查询角色列表失败", e);
            return ApiResponse.error("查询角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询角色树
     *
     * @param request 查询请求参数
     * @return 角色树查询结果
     */
    @PostMapping("/queryRoleTree")
    public ApiResponse<List<RoleDTO>> queryRoleTree(@RequestBody RoleRequest request) {
        try {
            BaseResponse<RoleDTO> response = roleService.queryRoleTree(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询角色树失败");
        } catch (Exception e) {
            log.error("查询角色树失败", e);
            return ApiResponse.error("查询角色树失败：" + e.getMessage());
        }
    }

    /**
     * 查询角色下的用户
     *
     * @param request 查询请求参数
     * @return 角色下的用户查询结果
     */
    @PostMapping("/queryRoleUsers")
    public ApiResponse<List<UserDTO>> queryRoleUsers(@RequestBody RoleRequest request) {
        try {
            BaseResponse<UserDTO> response = roleService.queryRoleUsers(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询角色下的用户失败");
        } catch (Exception e) {
            log.error("查询角色下的用户失败", e);
            return ApiResponse.error("查询角色下的用户失败：" + e.getMessage());
        }
    }

    /**
     * 查询角色下的用户ID列表
     *
     * @param request 查询请求参数
     * @return 角色下的用户ID列表查询结果
     */
    @PostMapping("/queryRoleUserIds")
    public ApiResponse<List<Long>> queryRoleUserIds(@RequestBody RoleRequest request) {
        try {
            BaseResponse<Long> response = roleService.queryRoleUserIds(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询角色下的用户ID列表失败");
        } catch (Exception e) {
            log.error("查询角色下的用户ID列表失败", e);
            return ApiResponse.error("查询角色下的用户ID列表失败：" + e.getMessage());
        }
    }
} 

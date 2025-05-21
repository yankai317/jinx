package com.learn.controller.role;

import com.learn.dto.common.ApiResponse;
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
import com.learn.service.role.RoleManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/role")
@Slf4j
public class RoleManageController {

    @Autowired
    private RoleManageService roleManageService;

    /**
     * 获取角色列表
     *
     * @param request 查询请求
     * @return 角色列表
     */
    @GetMapping("/list")
    public ApiResponse<RoleListResponse> getRoleList(RoleListRequest request) {
        try {
            RoleListResponse response = roleManageService.getRoleList(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取角色列表失败", e);
            return ApiResponse.error("获取角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建角色
     *
     * @param request 创建请求
     * @return 创建的角色信息
     */
    @PostMapping("/create")
    public ApiResponse<RoleDTO> createRole(@RequestBody RoleCreateRequest request) {
        try {
            RoleDTO roleDTO = roleManageService.createRole(request);
            return ApiResponse.success("创建成功", roleDTO);
        } catch (Exception e) {
            log.error("创建角色失败", e);
            return ApiResponse.error("创建角色失败：" + e.getMessage());
        }
    }

    /**
     * 更新角色
     *
     * @param request 更新请求
     * @return 是否成功
     */
    @PostMapping("/update")
    public ApiResponse<Boolean> updateRole(@RequestBody RoleUpdateRequest request) {
        try {
            Boolean result = roleManageService.updateRole(request);
            return ApiResponse.success("修改成功", result);
        } catch (Exception e) {
            log.error("更新角色失败", e);
            return ApiResponse.error("更新角色失败：" + e.getMessage());
        }
    }

    /**
     * 删除角色
     *
     * @param request 删除请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public ApiResponse<Boolean> deleteRole(@RequestBody RoleDeleteRequest request) {
        try {
            Boolean result = roleManageService.deleteRole(request);
            return ApiResponse.success("删除成功", result);
        } catch (Exception e) {
            log.error("删除角色失败", e);
            return ApiResponse.error("删除角色失败：" + e.getMessage());
        }
    }

    /**
     * 给角色添加用户
     *
     * @param request 添加用户请求
     * @return 添加结果
     */
    @PostMapping("/addUsers")
    public ApiResponse<RoleAddUsersResponse> addUsersToRole(@RequestBody RoleAddUsersRequest request) {
        try {
            RoleAddUsersResponse response = roleManageService.addUsersToRole(request);
            return ApiResponse.success("添加成功", response);
        } catch (Exception e) {
            log.error("给角色添加用户失败", e);
            return ApiResponse.error("给角色添加用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取角色下的用户列表
     *
     * @param request 查询请求
     * @return 用户列表
     */
    @GetMapping("/users")
    public ApiResponse<RoleUsersResponse> getRoleUsers(RoleUsersRequest request) {
        try {
            RoleUsersResponse response = roleManageService.getRoleUsers(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取角色下的用户列表失败", e);
            return ApiResponse.error("获取角色下的用户列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 从角色中移除用户
     *
     * @param request 移除用户请求
     * @return 是否成功
     */
    @PostMapping("/removeUsers")
    public ApiResponse<Boolean> removeUsersFromRole(@RequestBody RoleRemoveUsersRequest request) {
        try {
            Boolean result = roleManageService.removeUsersFromRole(request);
            return ApiResponse.success("移除成功", result);
        } catch (Exception e) {
            log.error("从角色中移除用户失败", e);
            return ApiResponse.error("从角色中移除用户失败：" + e.getMessage());
        }
    }
}

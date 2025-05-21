package com.learn.controller.org;

import com.learn.dto.common.ApiResponse;
import com.learn.service.dto.base.BaseResponse;
import com.learn.service.dto.org.OrgDTO;
import com.learn.service.dto.org.OrgRequest;
import com.learn.service.dto.user.UserDTO;
import com.learn.service.org.OrgService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织控制器
 */
@RestController
@RequestMapping("/api/org")
@CrossOrigin
@Slf4j
public class OrgController {

    private static final Logger logger = LoggerFactory.getLogger(OrgController.class);

    @Autowired
    private OrgService orgService;

    /**
     * 查询部门列表
     *
     * @param request 查询请求参数
     * @return 部门列表查询结果
     */
    @PostMapping("/queryOrgs")
    public ApiResponse<List<OrgDTO>> queryOrgs(@RequestBody OrgRequest request) {
        try {
            BaseResponse<OrgDTO> response = orgService.queryOrgs(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询部门列表失败");
        } catch (Exception e) {
            logger.error("查询部门列表失败", e);
            return ApiResponse.error("查询部门列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询部门树
     *
     * @param request 查询请求参数
     * @return 部门树查询结果
     */
    @PostMapping("/queryOrgTree")
    public ApiResponse<OrgDTO> queryOrgTree(@RequestBody OrgRequest request) {
        try {
            BaseResponse<OrgDTO> response = orgService.queryOrgTree(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getItem());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询部门树失败");
        } catch (Exception e) {
            logger.error("查询部门树失败", e);
            return ApiResponse.error("查询部门树失败：" + e.getMessage());
        }
    }

    /**
     * 查询部门下的用户
     *
     * @param request 查询请求参数
     * @return 部门下的用户查询结果
     */
    @PostMapping("/queryOrgUsers")
    public ApiResponse<List<UserDTO>> queryOrgUsers(@RequestBody OrgRequest request) {
        try {
            BaseResponse<UserDTO> response = orgService.queryOrgUsers(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询部门下的用户失败");
        } catch (Exception e) {
            logger.error("查询部门下的用户失败", e);
            return ApiResponse.error("查询部门下的用户失败：" + e.getMessage());
        }
    }

    /**
     * 查询部门下的用户ID列表
     *
     * @param request 查询请求参数
     * @return 部门下的用户ID列表查询结果
     */
    @PostMapping("/queryOrgUserIds")
    public ApiResponse<List<Long>> queryOrgUserIds(@RequestBody OrgRequest request) {
        try {
            BaseResponse<Long> response = orgService.queryOrgUserIds(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询部门下的用户ID列表失败");
        } catch (Exception e) {
            logger.error("查询部门下的用户ID列表失败", e);
            return ApiResponse.error("查询部门下的用户ID列表失败：" + e.getMessage());
        }
    }

    /**
     * 查询部门的子部门
     *
     * @param request 查询子部门请求参数
     * @return 子部门查询结果
     */
    @PostMapping("/queryOrgChildren")
    public ApiResponse<List<OrgDTO>> queryOrgChildren(@RequestBody OrgRequest request) {
        try {
            BaseResponse<OrgDTO> response = orgService.queryOrgChildren(request);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "查询部门的子部门失败");
        } catch (Exception e) {
            logger.error("查询部门的子部门失败", e);
            return ApiResponse.error("查询部门的子部门失败：" + e.getMessage());
        }
    }

    /**
     * 批量获取用户的部门信息
     *
     * @param userIds 用户ID列表
     * @return 用户部门信息列表
     */
    @PostMapping("/queryUserOrgs")
    public ApiResponse<List<UserDTO>> queryUserOrgs(@RequestBody List<Long> userIds) {
        try {
            BaseResponse<UserDTO> response = orgService.queryUserOrgs(userIds);
            if (response != null && response.isSuccess()) {
                return ApiResponse.success(response.getData());
            }
            return ApiResponse.error(response != null ? response.getMessage() : "获取用户部门信息失败");
        } catch (Exception e) {
            logger.error("获取用户部门信息失败", e);
            return ApiResponse.error("获取用户部门信息失败：" + e.getMessage());
        }
    }
} 

package com.learn.controller.common;

import com.learn.common.dto.UserTokenInfo;
import com.learn.common.util.JwtUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.common.RangeQueryResponse;
import com.learn.service.common.range.handler.RangeHandlerChain;
import com.learn.service.CommonRangeInterface;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeDeleteRequest;
import com.learn.service.dto.CommonRangeQueryRequest;
import com.learn.service.dto.CommonRangeQueryResponse;
import com.learn.service.dto.CommonRangeUpdateRequest;
import com.learn.service.dto.CommonRangeUpdateResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.learn.constants.BizConstants.DEFAULT_USER_ID;

/**
 * 通用范围控制器
 * 处理可见范围、协同管理、任务指派等功能的通用范围配置
 */
@RestController
@RequestMapping("/api/common/range")
@CrossOrigin
@Slf4j
public class CommonRangeController {

    @Autowired
    private CommonRangeInterface commonRangeInterface;


    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RangeHandlerChain rangeHandlerChain;

    /**
     * 查询范围配置
     *
     * @param businessType 业务类型（如course, train, learnMap）
     * @param businessId 业务ID
     * @param functionType 功能类型（如visibility, collaborate, assign）
     * @return 范围配置查询结果
     */
    @GetMapping("/query")
    public ApiResponse<RangeQueryResponse> queryRange(
            @RequestParam String businessType,
            @RequestParam Long businessId,
            @RequestParam String functionType) {
        try {
            log.info("查询范围配置，业务类型：{}，业务ID：{}，功能类型：{}", businessType, businessId, functionType);

            // 构建查询请求
            CommonRangeQueryRequest request = new CommonRangeQueryRequest();
            request.setModelType(functionType);
            request.setType(businessType);
            request.setTypeId(businessId);

            // 查询范围配置
            CommonRangeQueryResponse response = commonRangeInterface.queryRangeConfigByBusinessId(request);

            if (Objects.isNull(response)
                    || Objects.equals(response.getSuccess(), Boolean.FALSE)) {
                return ApiResponse.error("查询范围配置失败");
            }

            // 使用责任链模式处理范围信息
            RangeQueryResponse result = rangeHandlerChain.process(response);

            // 设置范围类型
            if (result.getType() == null) {
                // 判断范围类型
                List<Long> userIds = result.getUserInfos().stream()
                        .map(RangeQueryResponse.RangeUser::getUserId)
                        .collect(Collectors.toList());
                if (userIds.contains(DEFAULT_USER_ID)) {
                    // 如果没有配置任何范围，默认为全员可见
                    result.setType("ALL");
                }

            }
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询范围配置失败", e);
            return ApiResponse.error("查询范围配置失败：" + e.getMessage());
        }
    }

    /**
     * 更新范围配置
     *
     * @param request 更新请求参数
     * @param httpRequest HTTP请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public ApiResponse<Object> updateRange(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        try {
            log.info("更新范围配置，请求参数：{}", request);

            // 获取当前用户信息
            Long userId = getCurrentUserId(httpRequest);
            if (userId == null) {
                return ApiResponse.error(401, "未登录或登录已过期");
            }

            // 解析请求参数
            String modelType = (String) request.get("modelType");
            String type = (String) request.get("type");
            Long typeId = Long.valueOf(request.get("typeId").toString());
            Map<String, Object> targetTypeAndIds = (Map<String, Object>) request.get("targetTypeAndIds");

            if (StringUtils.isEmpty(modelType) || StringUtils.isEmpty(type) || typeId == null) {
                return ApiResponse.error(400, "参数不能为空");
            }

            // 构建更新请求
            CommonRangeUpdateRequest updateRequest = new CommonRangeUpdateRequest();
            updateRequest.setModelType(modelType);
            updateRequest.setType(type);
            updateRequest.setTypeId(typeId);
            updateRequest.setUpdaterId(userId);
            updateRequest.setUpdaterName("用户" + userId);

            // 处理目标类型和ID
            Map<String, java.util.List<Long>> targetMap = new HashMap<>();
            
            // 处理特殊情况：我的管理范围
            if (targetTypeAndIds.containsKey("myManage")) {
                // 我的管理范围使用特殊ID 0 表示
                java.util.List<Long> specialIds = new java.util.ArrayList<>();
                specialIds.add(0L);
                targetMap.put("department", specialIds);
            } else {
                // 处理普通情况：部门、角色、用户
                for (Map.Entry<String, Object> entry : targetTypeAndIds.entrySet()) {
                    String targetType = entry.getKey();
                    if (entry.getValue() instanceof java.util.List) {
                        java.util.List<Integer> ids = (java.util.List<Integer>) entry.getValue();
                        java.util.List<Long> longIds = new java.util.ArrayList<>();
                        for (Integer id : ids) {
                            longIds.add(Long.valueOf(id));
                        }
                        targetMap.put(targetType, longIds);
                    }
                }
            }
            
            updateRequest.setTargetTypeAndIds(targetMap);

            // 更新范围配置
            CommonRangeUpdateResponse response = commonRangeInterface.updateRangeByBusinessId(updateRequest);

            if (response != null && response.getSuccess()) {
                return ApiResponse.success("更新范围配置成功");
            }

            return ApiResponse.error("更新范围配置失败");
        } catch (Exception e) {
            log.error("更新范围配置失败", e);
            return ApiResponse.error("更新范围配置失败：" + e.getMessage());
        }
    }

    /**
     * 删除范围配置
     *
     * @param request 删除请求参数
     * @return 删除结果
     */
    @PostMapping("/delete")
    public ApiResponse<Object> deleteRange(@RequestBody Map<String, Object> request) {
        try {
            log.info("删除范围配置，请求参数：{}", request);

            // 解析请求参数
            String modelType = (String) request.get("modelType");
            String type = (String) request.get("type");
            Long typeId = Long.valueOf(request.get("typeId").toString());

            if (StringUtils.isEmpty(modelType) || StringUtils.isEmpty(type) || typeId == null) {
                return ApiResponse.error(400, "参数不能为空");
            }

            // 构建删除请求
            CommonRangeDeleteRequest deleteRequest = new CommonRangeDeleteRequest();
            deleteRequest.setModelType(modelType);
            deleteRequest.setType(type);
            deleteRequest.setTypeId(typeId);

            // 删除范围配置
            Boolean result = commonRangeInterface.deleteRangeByBusinessId(deleteRequest);

            if (Boolean.TRUE.equals(result)) {
                return ApiResponse.success("删除范围配置成功");
            }

            return ApiResponse.error("删除范围配置失败");
        } catch (Exception e) {
            log.error("删除范围配置失败", e);
            return ApiResponse.error("删除范围配置失败：" + e.getMessage());
        }
    }

    /**
     * 从请求中获取当前用户ID
     *
     * @param request HTTP请求
     * @return 用户ID，如果未登录则返回null
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        // 从请求头中获取token
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        // 提取token
        String token = authHeader.substring(7);

        // 从token中获取用户ID
        try {
            UserTokenInfo userInfo = jwtUtil.extractUserId(token);
            return userInfo.getUserId();
        } catch (Exception e) {
            log.error("从token中提取用户ID失败", e);
            return null;
        }
    }
}

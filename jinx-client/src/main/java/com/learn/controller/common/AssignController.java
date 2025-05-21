package com.learn.controller.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.common.dto.UserTokenInfo;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.enums.AssignTypeEnums;
import com.learn.common.enums.RangeModelTypeEnums;
import com.learn.common.enums.RangeTargetTypeEnums;
import com.learn.common.exception.CommonException;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.common.*;
import com.learn.dto.user.UserInfoResponse;
import com.learn.infrastructure.repository.entity.AssignRecords;
import com.learn.infrastructure.repository.entity.User;
import com.learn.infrastructure.repository.entity.UserThirdParty;
import com.learn.infrastructure.repository.mapper.UserMapper;
import com.learn.infrastructure.repository.mapper.UserThirdPartyMapper;
import com.learn.service.assignment.AssignmentDetailService;
import com.learn.service.common.strategy.AssignStrategy;
import com.learn.service.common.strategy.AssignStrategyFactory;
import com.learn.service.dto.CommonRangeCreateRequest;
import com.learn.service.dto.CommonRangeCreateResponse;
import com.learn.service.map.LearnMapAssignService;
import com.learn.service.operation.OperationLogService;
import com.learn.service.train.TrainAssignService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 指派控制器
 * 处理课程、培训、学习地图的指派操作
 *
 * @author yujintao
 * @date 2025/4/22
 */
@RestController
@RequestMapping("/api/common")
@Slf4j
public class AssignController {

    @Autowired
    private UserTokenUtil userTokenUtil;

    @Autowired
    private AssignmentDetailService assignmentDetailService;

    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignStrategyFactory assignStrategyFactory;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserThirdPartyMapper userThirdPartyMapper;

    @PostMapping("/assign")
    public ApiResponse<AssignResponse> assign(@RequestBody AssignRequest request, HttpServletRequest httpRequest) {
        log.info("指派业务给用户，请求参数：{}", request);

        // 参数校验
        if (request.getBizId() == null || request.getBizType() == null) {
            return ApiResponse.error(400, "业务ID不能为空");
        }

        // 获取当前用户信息
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);

        // BizType 转大写
        request.setBizType(request.getBizType().toUpperCase());
        
        // 处理入参转换
        processRequestUserIds(request);
        
        // 至少需要指定用户、部门或角色中的一种
        if (CollectionUtils.isEmpty(request.getUserIds()) 
                && CollectionUtils.isEmpty(request.getDepartmentIds()) 
                && CollectionUtils.isEmpty(request.getRoleIds())) {
            return ApiResponse.error(400, "用户ID、部门ID或角色ID列表至少需要提供一种");
        }


        try {
            // 设置操作人信息
            request.setOperatorId(userInfo.getUserId());
            request.setOperatorName(userInfo.getNickname());
            
            // 获取对应的指派策略
            AssignStrategy assignStrategy = assignStrategyFactory.getStrategy(request.getBizType());
            
//            // 检查权限
//            if (!assignStrategy.checkPermission(request, userInfo.getUserId())) {
//                return ApiResponse.error(403, "没有权限执行指派操作");
//            }

            // 调用对应的策略执行指派操作
            AssignResponse response = assignStrategy.assign(request);
            
            // 特殊处理：当指派类型为auto且包含assignRecordId时，更新现有记录而不是创建新记录
            if (AssignTypeEnums.AUTO.getCode().equalsIgnoreCase(request.getAssignType())
                    && request.getAssignRecordId() != null) {
                // 获取现有的assign_record记录
                AssignRecords existingRecord = assignmentDetailService.getAssignRecordById(request.getAssignRecordId());
                if (existingRecord != null) {
                    // 更新范围IDs
                    existingRecord.setRangeIds(new ArrayList<>(response.getRangeIds()).toString().replace(" ", ""));
                    existingRecord.setAssignFinishedTimeType(request.getAssignFinishedTimeType());
                    existingRecord.setCustomFinishedDay(request.getCustomFinishedDay());
                    existingRecord.setIfIsNotifyExistUser(request.getIfIsNotifyExistUser());
                    existingRecord.setNotifyUserAfterJoinDate(request.getNotifyUserAfterJoinDate());
                    existingRecord.setUpdaterId(userInfo.getUserId());
                    existingRecord.setUpdaterName(userInfo.getNickname());
                    existingRecord.setGmtModified(new Date());
                    existingRecord.setDeadline(response.getAssignEndTime());
                    
                    // 更新记录
                    assignmentDetailService.updateAssignRecord(existingRecord);
                    
                    // 设置assignRecordId
                    response.setAssignRecordId(existingRecord.getId());
                } else {
                    log.warn("未找到指定的指派记录ID: {}, 将创建新记录", request.getAssignRecordId());
                    // 创建新记录
                    createNewAssignRecord(request, response, userInfo);
                }
            } else {
                // 原有逻辑：创建新的指派记录
                createNewAssignRecord(request, response, userInfo);
            }

            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            String operationDetail = objectMapper.writeValueAsString(request);
            operationLogService.recordAssignOperation(request.getBizId(), request.getBizType(), operationDetail, tokenInfo);

            return ApiResponse.success("指派成功", response);
        } catch (Exception e) {
            log.error("指派失败", e);
            return ApiResponse.error(500, "指派失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建新的指派记录
     */
    private void createNewAssignRecord(AssignRequest request, AssignResponse response, UserInfoResponse userInfo) {
        // 将范围ID + 业务id + 业务类型 存储到 assign_records 表中
        if (response.getRangeIds() != null && !response.getRangeIds().isEmpty()) {
            AssignRecords assignRecord = assignmentDetailService.saveAssignRecord(request,
                    new ArrayList<>(response.getRangeIds()),
                    userInfo.getUserId(),
                    userInfo.getNickname(),
                    response.getAssignEndTime()
            );

            // 设置指派记录ID
            response.setAssignRecordId(assignRecord.getId());
        } else {
            log.warn("指派策略未返回范围ID，无法创建指派记录");
        }
    }

    /**
     * 处理请求中的用户ID转换
     * 1. 如果request中workNos不为空，则去user表中查询到userId
     * 2. 如果request中的thirtyUserIds不为空，则thirtyType必填，使用这两个参数去user_third_party表中查询到userId
     * 3. 将入参中的userIds和转换后的userIds都合并到一起，设置到入参中的userIds中
     *
     * @param request 指派请求
     */
    private void processRequestUserIds(AssignRequest request) {
        // 存储所有需要合并的用户ID
        Set<Long> mergedUserIds = new HashSet<>();
        
        // 如果原始userIds不为空，先添加到合并集合中
        if (!CollectionUtils.isEmpty(request.getUserIds())) {
            mergedUserIds.addAll(request.getUserIds());
        }
        
        // 处理工号查询
        if (!CollectionUtils.isEmpty(request.getWorkNos())) {
            LambdaQueryWrapper<User> userQueryWrapper = new LambdaQueryWrapper<>();
            userQueryWrapper.in(User::getEmployeeNo, request.getWorkNos())
                    .eq(User::getIsDel, 0); // 只查询未删除的用户
            
            List<User> users = userMapper.selectList(userQueryWrapper);
            if (!CollectionUtils.isEmpty(users)) {
                List<Long> userIdsByWorkNo = users.stream()
                        .map(User::getUserId)
                        .collect(Collectors.toList());
                mergedUserIds.addAll(userIdsByWorkNo);
            }
        }
        
        // 处理第三方用户ID查询
        if (!CollectionUtils.isEmpty(request.getThirtyUserIds())) {
            // 校验thirtyType是否填写
            if (StringUtils.isEmpty(request.getThirtyType())) {
                throw new CommonException("当使用第三方用户ID时，第三方平台类型(thirtyType)必须填写");
            }
            
            LambdaQueryWrapper<UserThirdParty> thirdPartyQueryWrapper = new LambdaQueryWrapper<>();
            thirdPartyQueryWrapper.in(UserThirdParty::getThirdPartyUserId, request.getThirtyUserIds())
                    .eq(UserThirdParty::getThirdPartyType, request.getThirtyType())
                    .eq(UserThirdParty::getIsDel, 0); // 只查询未删除的关联
            
            List<UserThirdParty> userThirdParties = userThirdPartyMapper.selectList(thirdPartyQueryWrapper);
            if (!CollectionUtils.isEmpty(userThirdParties)) {
                List<Long> userIdsByThirdParty = userThirdParties.stream()
                        .map(UserThirdParty::getUserId)
                        .collect(Collectors.toList());
                mergedUserIds.addAll(userIdsByThirdParty);
            }
        }
        
        // 将合并后的用户ID设置回请求对象
        if (!mergedUserIds.isEmpty()) {
            request.setUserIds(new ArrayList<>(mergedUserIds));
        }
    }
    
    /**
     * 查询指派记录
     *
     * @param businessId 业务ID
     * @param request 查询请求参数
     * @return 指派记录列表及分页信息
     */
    @PostMapping("/assign/records/{businessId}")
    public ApiResponse<Map<String, Object>> queryAssignRecords(@PathVariable("businessId") Long businessId,
                                                              @RequestBody AssignRecordRequest request) {
        log.info("查询业务ID[{}]的指派记录，参数：{}", businessId, request);

        if (businessId == null) {
            return ApiResponse.error(400, "业务ID不能为空");
        }

        try {
            // 查询指派记录
            List<AssignRecordResponse> records = assignmentDetailService.queryAssignRecords(businessId, request);

            // 统计总记录数
            Long total = assignmentDetailService.countAssignRecords(businessId, request);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("list", records);
            result.put("total", total);
            result.put("pageNum", request.getPageNum());
            result.put("pageSize", request.getPageSize());

            return ApiResponse.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询指派记录失败", e);
            return ApiResponse.error(500, "查询指派记录失败：" + e.getMessage());
        }
    }
}

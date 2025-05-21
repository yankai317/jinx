package com.learn.controller.train;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learn.common.dto.UserTokenInfo;
import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.JwtUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.constants.BizType;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.common.RangeBaseRequest;
import com.learn.dto.course.sub.VisibilityDTO;
import com.learn.dto.train.TrainVisibilityRequest;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.common.range.RangeSetService;
import com.learn.service.operation.OperationLogService;
import com.learn.service.train.TrainVisibilityService;
import com.learn.service.user.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 培训可见范围控制器
 */
@RestController
@RequestMapping("/api/train/visibility")
@CrossOrigin
@Slf4j
public class TrainVisibilityController {

    @Autowired
    private RangeSetService rangeSetService;

    @Autowired
    private TrainVisibilityService trainVisibilityService;



    @Autowired
    private UserTokenUtil userTokenUtil;

    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 设置培训可见范围
     *
     * @param trainId 培训ID
     * @param request 可见范围设置请求
     * @param httpRequest HTTP请求
     * @return 设置结果
     */
    @PostMapping("/{trainId}")
    public ApiResponse<Boolean> setTrainVisibility(
            @PathVariable("trainId") Long trainId,
            @RequestBody VisibilityDTO request,
            HttpServletRequest httpRequest) {
        log.info("设置培训可见范围，培训ID：{}，请求参数：{}", trainId, request);
        
        // 参数校验
        if (trainId == null || trainId <= 0) {
            return ApiResponse.error(400, "培训ID不能为空或小于等于0");
        }
        
        if (request == null) {
            return ApiResponse.error(400, "可见范围设置请求不能为空");
        }
        
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            return ApiResponse.error(400, "可见范围类型不能为空");
        }
        
        // 校验可见范围类型
        if (!"ALL".equals(request.getType()) && !"PART".equals(request.getType())) {
            return ApiResponse.error(400, "可见范围类型无效，有效值为：ALL, PART");
        }
        
        // 校验部分可见时必须指定可见目标
        if ("PART".equals(request.getType()) && 
            (request.getTargets() == null || request.getTargets().isEmpty())) {
            return ApiResponse.error(400, "部分可见时必须指定可见目标");
        }
        
        try {
            // 获取用户详细信息
            UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);
            String userName = userInfo.getNickname();
            Long userId = userInfo.getUserId();

            RangeBaseRequest rangeBaseRequest = new RangeBaseRequest();
            rangeBaseRequest.setVisibility(request);
            rangeBaseRequest.setOperatorId(userId);
            rangeBaseRequest.setOperatorName(userName);
            rangeSetService.updateVisibilityRange(BizType.TRAIN, trainId, rangeBaseRequest);

            // 记录操作日志
            UserTokenInfo tokenInfo = UserContextHolder.getUserInfo();
            try {
                String operationDetail = objectMapper.writeValueAsString(request);
                operationLogService.recordUpdateOperation(trainId, BizType.TRAIN, operationDetail, tokenInfo);
            } catch (JsonProcessingException e) {
                log.error("记录培训可见范围设置操作日志失败", e);
            }
            
            return ApiResponse.success("设置成功", true);
        } catch (Exception e) {
            log.error("设置培训可见范围失败", e);
            return ApiResponse.error(500, "设置培训可见范围失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取培训可见范围
     *
     * @param trainId 培训ID
     * @return 可见范围设置
     */
    @GetMapping("/{trainId}")
    public ApiResponse<TrainVisibilityRequest> getTrainVisibility(@PathVariable("trainId") Long trainId) {
        log.info("获取培训可见范围，培训ID：{}", trainId);
        
        // 参数校验
        if (trainId == null || trainId <= 0) {
            return ApiResponse.error(400, "培训ID不能为空或小于等于0");
        }
        
        try {
            // 调用服务获取培训可见范围
            TrainVisibilityRequest visibility = trainVisibilityService.getTrainVisibility(trainId);
            
            return ApiResponse.success("获取成功", visibility);
        } catch (Exception e) {
            log.error("获取培训可见范围失败", e);
            return ApiResponse.error(500, "获取培训可见范围失败：" + e.getMessage());
        }
    }

}

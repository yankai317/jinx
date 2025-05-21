package com.learn.controller.train;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.train.TrainLearnerDetailDTO;
import com.learn.service.train.TrainUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 培训用户控制器
 */
@RestController
@RequestMapping("/api/train/learner")
@Slf4j
public class TrainUserController {

    @Autowired
    private TrainUserService trainUserService;

    /**
     * 获取培训学员学习详情
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 学习详情
     */
    @GetMapping("/user/detail")
    public ApiResponse<TrainLearnerDetailDTO> getTrainLearnerDetail(
            @RequestParam("trainId") Long trainId,
            @RequestParam("userId") Long userId) {
        log.info("获取培训学员学习详情，培训ID：{}，用户ID：{}", trainId, userId);
        // 参数校验
        if (trainId == null || trainId <= 0) {
            return ApiResponse.error("培训ID不能为空");
        }
        if (userId == null || userId <= 0) {
            return ApiResponse.error("用户ID不能为空");
        }
        
        // 调用服务获取学习详情
        TrainLearnerDetailDTO detailDTO = trainUserService.getTrainLearnerDetail(trainId, userId);
        
        if (detailDTO == null) {
            return ApiResponse.error("未找到学员学习记录");
        }
        
        return ApiResponse.success(detailDTO);
    }
    
    /**
     * 发送学习提醒
     *
     * @param trainId 培训ID
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/remind")
    public ApiResponse<Boolean> sendLearningReminder(
            @RequestParam("trainId") Long trainId,
            @RequestParam("userId") Long userId) {
        log.info("发送学习提醒，培训ID：{}，用户ID：{}", trainId, userId);
        // 参数校验
        if (trainId == null || trainId <= 0) {
            return ApiResponse.error("培训ID不能为空");
        }
        if (userId == null || userId <= 0) {
            return ApiResponse.error("用户ID不能为空");
        }
        
        // 调用服务发送学习提醒
        boolean success = trainUserService.sendLearningReminder(trainId, userId);
        
        if (!success) {
            return ApiResponse.error("发送学习提醒失败");
        }
        
        return ApiResponse.success("发送学习提醒成功", true);
    }
}

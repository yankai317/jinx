package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.common.util.UserContextHolder;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.train.TrainDetailResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.TrainService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C端培训控制器
 */
@RestController
@RequestMapping("/api/train")
@Slf4j
public class TocTrainController {

    @Autowired
    private TrainService trainService;

    @Autowired
    private UserTokenUtil userTokenUtil;

    /**
     * 获取培训详情
     *
     * @param id 培训ID
     * @param request HTTP请求
     * @return 培训详情
     */
    @GetMapping("/detail")
    public ApiResponse<TrainDetailResponse> getTrainDetail(@RequestParam("id") Long id) {
        log.info("获取培训详情，培训ID：{}", id);
        
        // 参数校验
        if (id == null || id <= 0) {
            log.error("培训ID参数无效：{}", id);
            return ApiResponse.error(400, "培训ID参数无效");
        }
        
        // 获取当前用户信息
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            log.error("用户未登录");
            return ApiResponse.error(401, "用户未登录");
        }
        
        // 调用服务获取培训详情
        TrainDetailResponse trainDetail = trainService.getTrainDetail(id, userId);
        if (trainDetail == null) {
            return ApiResponse.error(404, "培训不存在或已删除");
        }
        
        return ApiResponse.success(trainDetail);
    }
}

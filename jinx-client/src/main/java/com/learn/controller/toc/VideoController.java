package com.learn.controller.toc;

import com.learn.common.dto.util.UserTokenUtil;
import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.learning.RecordLearningProgressRequest;
import com.learn.dto.toc.learning.RecordLearningProgressResponse;
import com.learn.dto.toc.share.ShareContentResponse;
import com.learn.dto.toc.video.VideoPlayRequest;
import com.learn.dto.toc.video.VideoPlayResponse;
import com.learn.dto.user.UserInfoResponse;
import com.learn.service.toc.LearningProgressService;
import com.learn.service.toc.ShareService;
import com.learn.service.toc.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 视频播放控制器
 */
@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class VideoController {

    private final VideoService videoService;
    private final LearningProgressService learningProgressService;
    private final ShareService shareService;

    @Autowired
    UserTokenUtil userTokenUtil;

    /**
     * 获取视频播放信息
     *
     * @param request 视频播放请求
     * @param httpRequest HTTP请求
     * @return 视频播放信息
     */
    @GetMapping("/play")
    public ApiResponse<VideoPlayResponse> getVideoPlayInfo(VideoPlayRequest request, HttpServletRequest httpRequest) {
        log.info("获取视频播放信息请求: {}", request);
        
        // 获取用户信息
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);
        if (userInfo == null) {
            return ApiResponse.error(401, "用户未登录");
        }

        // 参数校验
        if (request.getCourseId() == null) {
            return ApiResponse.error(400, "课程ID不能为空");
        }

        // 调用服务获取视频播放信息
        return videoService.getVideoPlayInfo(request, userInfo.getUserId());
    }
    
    /**
     * 记录视频学习进度
     *
     * @param request 记录学习进度请求
     * @param httpRequest HTTP请求
     * @return 记录结果
     */
    @PostMapping("/learning/record")
    public ApiResponse<Map<String, Object>> recordLearningProgress(
            @RequestBody RecordLearningProgressRequest request,
            HttpServletRequest httpRequest) {
        log.info("记录视频学习进度请求: {}", request);
        
        // 获取用户信息
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);
        if (userInfo == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        
        // 参数校验
        if (request.getContentType() == null || request.getContentType().isEmpty()) {
            return ApiResponse.error(400, "内容类型不能为空");
        }
        if (request.getContentId() == null) {
            return ApiResponse.error(400, "内容ID不能为空");
        }
        if (request.getProgress() == null) {
            return ApiResponse.error(400, "学习进度不能为空");
        }
        if (request.getDuration() == null) {
            return ApiResponse.error(400, "学习时长不能为空");
        }
        
        // 验证内容是否存在
        if (!learningProgressService.validateContent(request)) {
            return ApiResponse.error(400, "内容不存在");
        }
        
        try {
            // 调用服务记录学习进度
            RecordLearningProgressResponse response = learningProgressService.recordProgress(
                    userInfo.getUserId(), request);
            
            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("status", "completed".equals(response.getStatus()) ? 1 : 0);
            data.put("progress", response.getProgress());
            data.put("isCompleted", "completed".equals(response.getStatus()));
            data.put("studyDuration", response.getStudyDuration());
            
            return ApiResponse.success("记录成功", data);
        } catch (Exception e) {
            log.error("记录学习进度失败", e);
            return ApiResponse.error(500, "记录学习进度失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取视频分享信息
     *
     * @param type 内容类型
     * @param id 内容ID
     * @param httpRequest HTTP请求
     * @return 分享信息
     */
    @GetMapping("/share")
    public ApiResponse<ShareContentResponse> shareContent(
            @RequestParam String type,
            @RequestParam Integer id,
            HttpServletRequest httpRequest) {
        log.info("获取内容分享信息请求，type: {}, id: {}", type, id);
        
        // 验证用户token
        UserInfoResponse userInfo = userTokenUtil.getCurrentUserInfo(httpRequest);
        if (userInfo == null) {
            return ApiResponse.error(401, "用户未登录");
        }
        
        // 参数校验
        if (type == null || type.isEmpty()) {
            return ApiResponse.error(400, "内容类型不能为空");
        }
        if (id == null || id <= 0) {
            return ApiResponse.error(400, "内容ID不合法");
        }
        
        // 调用服务获取分享内容信息
        return shareService.getShareContent(type, id);
    }
}

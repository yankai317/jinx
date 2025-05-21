package com.learn.service.toc;

import com.learn.dto.common.ApiResponse;
import com.learn.dto.toc.video.VideoPlayRequest;
import com.learn.dto.toc.video.VideoPlayResponse;

/**
 * 视频服务接口
 */
public interface VideoService {
    
    /**
     * 获取视频播放信息
     * 
     * @param request 视频播放请求
     * @param userId 用户ID
     * @return 视频播放信息
     */
    ApiResponse<VideoPlayResponse> getVideoPlayInfo(VideoPlayRequest request, Long userId);
}

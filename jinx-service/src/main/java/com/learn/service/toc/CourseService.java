package com.learn.service.toc;

import com.learn.dto.toc.course.CourseDetailResponse;
import com.learn.dto.toc.video.VideoPlayRequest;
import com.learn.dto.toc.video.VideoPlayResponse;

/**
 * C端课程服务接口
 */
public interface CourseService {
    
    /**
     * 获取课程详情
     *
     * @param courseId 课程ID
     * @param userId 用户ID
     * @return 课程详情
     */
    CourseDetailResponse getCourseDetail(Long courseId, Long userId);
    
    /**
     * 获取视频播放信息
     *
     * @param request 视频播放请求
     * @param userId 用户ID
     * @return 视频播放信息
     */
    VideoPlayResponse getVideoPlayInfo(VideoPlayRequest request, Long userId);
}

package com.learn.dto.toc.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频播放响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayResponse {
    
    /**
     * 视频ID
     */
    private Long id;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 视频URL
     */
    private String videoUrl;
    
    /**
     * 封面图片URL
     */
    private String coverImage;
    
    /**
     * 视频总时长（秒）
     */
    private Integer duration;
    
    /**
     * 当前播放进度（秒）
     */
    private Integer progress;
    
    /**
     * 上次播放位置（秒）
     */
    private Integer lastPosition;
    
    /**
     * 下一个课程ID
     */
    private Long nextCourseId;
    
    /**
     * 上一个课程ID
     */
    private Long prevCourseId;
}

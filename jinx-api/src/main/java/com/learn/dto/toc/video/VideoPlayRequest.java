package com.learn.dto.toc.video;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频播放请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlayRequest {
    
    /**
     * 课程ID
     */
    private Integer courseId;
    
    /**
     * 培训ID（如果是培训中的视频）
     */
    private Integer trainId;
    
    /**
     * 学习地图ID（如果是学习地图中的视频）
     */
    private Integer mapId;
    
    /**
     * 阶段ID（如果是学习地图中的视频）
     */
    private Integer stageId;
}

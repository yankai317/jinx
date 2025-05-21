package com.learn.dto.learning;

import com.learn.constants.BizType;
import lombok.Data;

/**
 * @author yujintao
 * @date 2025/4/30
 */
@Data
public class LearningProgressRequest {
    /**
     * 业务类型
     * @see com.learn.constants.BizType
     */
    private String type;

    /**
     * 当前id，关联的培训id或学习地图阶段id
     */
    private Integer parentId;
    
    /**
     * 内容类型
     */
    private String contentType;


    /**
     * 内容id
     */
    private Integer contentId;

    /**
     * 进度
     */
    private Integer progress;

    /**
     * 学习时长
     */
    private Integer duration;
}

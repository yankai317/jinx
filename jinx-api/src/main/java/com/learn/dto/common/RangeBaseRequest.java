package com.learn.dto.common;

import com.learn.dto.course.sub.CollaboratorsDTO;
import com.learn.dto.course.sub.VisibilityDTO;
import lombok.Data;

/**
 * @author yujintao
 * @date 2025/4/29
 */
@Data
public class RangeBaseRequest {
    /**
     * 可见范围
     */
    private VisibilityDTO visibility;

    /**
     * 协同管理
     */
    private CollaboratorsDTO collaborators;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称
     */
    private String operatorName;
}

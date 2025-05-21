package com.learn.dto.course.sub;

import lombok.Data;

import java.util.List;

/**
 * @author yujintao
 * @date 2025/4/24
 */
@Data
public class VisibilityDTO {
    /**
     * 可见范围类型：ALL(所有人可见), PART(部分可见)
     */
    private String type;

    /**
     * 可见目标列表，type为PART时必填
     */
    private List<TargetDTO> targets;
}

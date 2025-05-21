package com.learn.dto.train;

import com.learn.dto.course.sub.TargetDTO;
import lombok.Data;

import java.util.List;

/**
 * 培训可见范围设置请求
 */
@Data
public class TrainVisibilityRequest {
    /**
     * 可见范围类型：ALL-所有人可见，PART-部分可见
     */
    private String type;
    
    /**
     * 可见目标列表
     */
    private List<TargetDTO> targets;
}

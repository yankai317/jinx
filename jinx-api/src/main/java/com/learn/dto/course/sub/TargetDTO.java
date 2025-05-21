package com.learn.dto.course.sub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yujintao
 * @date 2025/4/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TargetDTO {
    /**
     * 目标类型：department(部门), role(角色), user(用户)
     */
    private String type;

    /**
     * 目标ID列表
     */
    private List<Long> ids;


}

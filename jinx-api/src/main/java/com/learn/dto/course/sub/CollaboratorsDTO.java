package com.learn.dto.course.sub;

import lombok.Data;

import java.util.List;

/**
 * @author yujintao
 * @date 2025/4/24
 */
@Data
public class CollaboratorsDTO {

    private String editorType;

    /**
     * 编辑者ID列表
     */
    private List<Long> editors;

    private String userType;

    /**
     * 普通协作者ID列表
     */
    private List<Long> users;
}

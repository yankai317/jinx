package com.learn.dto.role;

import lombok.Data;

/**
 * 角色列表查询请求
 */
@Data
public class RoleListRequest {
    /**
     * 关键词，搜索角色名称
     */
    private String keyword;
    /**
     * 角色id
     */
    private Long id;
}

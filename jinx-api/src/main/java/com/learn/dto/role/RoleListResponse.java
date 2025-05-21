package com.learn.dto.role;

import lombok.Data;

import java.util.List;

/**
 * 角色列表查询响应
 */
@Data
public class RoleListResponse {
    /**
     * 角色列表
     */
    private List<RoleDTO> data;
}

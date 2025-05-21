package com.learn.dto.role;

/**
 * 获取角色下用户的请求
 */
public class RoleUsersRequest {
    /**
     * 角色ID
     */
    private Long id;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}

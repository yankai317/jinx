package com.learn.service.dto.role;

import com.learn.service.dto.base.BaseRequest;

/**
 * 角色请求对象
 */
public class RoleRequest extends BaseRequest {
    private Boolean includeChildren;
    private Boolean includeUsers;
    
    public Boolean getIncludeChildren() {
        return includeChildren;
    }
    
    public void setIncludeChildren(Boolean includeChildren) {
        this.includeChildren = includeChildren;
    }
    
    public Boolean getIncludeUsers() {
        return includeUsers;
    }
    
    public void setIncludeUsers(Boolean includeUsers) {
        this.includeUsers = includeUsers;
    }
} 
package com.learn.service.dto.org;

import com.learn.service.dto.user.UserDTO;
import java.util.List;

/**
 * 组织/部门DTO
 */
public class OrgDTO {
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private List<OrgDTO> children;
    private Boolean hasChildren;
    /**
     * 部门下的用户列表
     */
    private List<UserDTO> users;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }
    public Boolean getHasChildren() {
        return hasChildren;
    }
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public Long getParentId() {
        return parentId;
    }
    
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
    
    public List<OrgDTO> getChildren() {
        return children;
    }
    
    public void setChildren(List<OrgDTO> children) {
        this.children = children;
    }
    
    public List<UserDTO> getUsers() {
        return users;
    }
    
    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }
} 
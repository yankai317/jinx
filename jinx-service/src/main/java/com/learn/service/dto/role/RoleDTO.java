package com.learn.service.dto.role;

import java.util.List;

/**
 * 角色DTO
 */
public class RoleDTO {
    private Long id;
    private String name;
    private String code;
    private List<RoleDTO> children;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public List<RoleDTO> getChildren() {
        return children;
    }
    
    public void setChildren(List<RoleDTO> children) {
        this.children = children;
    }
} 
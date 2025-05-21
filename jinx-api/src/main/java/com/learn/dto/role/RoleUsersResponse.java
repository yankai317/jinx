package com.learn.dto.role;

import java.util.List;

/**
 * 获取角色下用户的响应
 */
public class RoleUsersResponse {
    /**
     * 用户列表
     */
    private List<UserDTO> data;
    
    public List<UserDTO> getData() {
        return data;
    }
    
    public void setData(List<UserDTO> data) {
        this.data = data;
    }
    
    /**
     * 用户DTO
     */
    public static class UserDTO {
        private Long id;
        private String username;
        private String nickname;
        private String email;
        private String phone;
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getNickname() {
            return nickname;
        }
        
        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPhone() {
            return phone;
        }
        
        public void setPhone(String phone) {
            this.phone = phone;
        }
    }
}

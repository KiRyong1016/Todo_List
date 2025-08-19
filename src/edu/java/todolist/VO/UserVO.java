package edu.java.todolist.VO;

import java.time.LocalDateTime;

public class UserVO {
    private int userId;
    private String email;
    private String password;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private boolean isActive;
    private String authProvider;

    public UserVO() {}

    public UserVO(int userId, String email, String password, String username,
                  LocalDateTime createdAt, LocalDateTime updatedAt,
                  LocalDateTime lastLogin, boolean isActive, String authProvider) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastLogin = lastLogin;
        this.isActive = isActive;
        this.authProvider = authProvider;
        if (authProvider == null) {
            this.authProvider = "local";
        } else {
            this.authProvider = authProvider;
        }
    }

    // Getter & Setter

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getAuthProvider() {
        return authProvider;
    }
    
    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public String toString() {
        return "UserVO{" +
                "userId=" + userId +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lastLogin=" + lastLogin +
                ", isActive=" + isActive +
                ", authProvider='" + authProvider + '\'' +
                '}';
    }
}

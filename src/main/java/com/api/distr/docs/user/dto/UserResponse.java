package com.api.distr.docs.user.dto;

import com.api.distr.docs.user.Role;
import com.api.distr.docs.user.User;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Role role;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserResponse() {}

    // ── Factory ──────────────────────────────────────────────────────────────

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id        = user.getId();
        r.username  = user.getUsername();
        r.fullName  = user.getFullName();
        r.email     = user.getEmail();
        r.role      = user.getRole();
        r.enabled   = user.isEnabled();
        r.createdAt = user.getCreatedAt();
        r.updatedAt = user.getUpdatedAt();
        return r;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public void setId(Long id)                   { this.id = id; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }

    public String getFullName()                  { return fullName; }
    public void setFullName(String fullName)     { this.fullName = fullName; }

    public String getEmail()                     { return email; }
    public void setEmail(String email)           { this.email = email; }

    public Role getRole()                        { return role; }
    public void setRole(Role role)               { this.role = role; }

    public boolean isEnabled()                   { return enabled; }
    public void setEnabled(boolean enabled)      { this.enabled = enabled; }

    public LocalDateTime getCreatedAt()                      { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)        { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt()                      { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt)        { this.updatedAt = updatedAt; }
}

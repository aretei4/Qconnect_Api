package com.api.distr.docs.user.dto;

import com.api.distr.docs.user.Role;

public class UserRequest {

    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private Role   role;

    public UserRequest() {}

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }

    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }

    public String getFullName()                  { return fullName; }
    public void setFullName(String fullName)     { this.fullName = fullName; }

    public String getEmail()                 { return email; }
    public void setEmail(String email)       { this.email = email; }

    public String getPhone()                 { return phone; }
    public void setPhone(String phone)       { this.phone = phone; }

    public Role getRole()            { return role; }
    public void setRole(Role role)   { this.role = role; }
}

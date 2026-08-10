package com.api.distr.docs.user.dto;

import com.api.distr.docs.user.Role;

public class UserUpdateRequest {

    private String  fullName;
    private String  email;
    private String  phone;
    private Role    role;
    private Boolean enabled;
    private String  password;

    public UserUpdateRequest() {}

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getFullName()                  { return fullName; }
    public void setFullName(String fullName)     { this.fullName = fullName; }

    public String getEmail()                 { return email; }
    public void setEmail(String email)       { this.email = email; }

    public String getPhone()                 { return phone; }
    public void setPhone(String phone)       { this.phone = phone; }

    public Role getRole()            { return role; }
    public void setRole(Role role)   { this.role = role; }

    public Boolean getEnabled()                  { return enabled; }
    public void setEnabled(Boolean enabled)      { this.enabled = enabled; }

    public String getPassword()                  { return password; }
    public void setPassword(String password)     { this.password = password; }
}

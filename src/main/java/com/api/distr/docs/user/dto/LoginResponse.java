package com.api.distr.docs.user.dto;

public class LoginResponse {

    private String token;
    private String tokenType;
    private Long   userId;
    private String username;
    private String fullName;
    private String role;

    public LoginResponse() {}

    public LoginResponse(String token, String tokenType, Long userId,
                         String username, String fullName, String role) {
        this.token     = token;
        this.tokenType = tokenType;
        this.userId    = userId;
        this.username  = username;
        this.fullName  = fullName;
        this.role      = role;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getToken()                 { return token; }
    public void setToken(String token)       { this.token = token; }

    public String getTokenType()                   { return tokenType; }
    public void setTokenType(String tokenType)     { this.tokenType = tokenType; }

    public Long getUserId()              { return userId; }
    public void setUserId(Long userId)   { this.userId = userId; }

    public String getUsername()                  { return username; }
    public void setUsername(String username)     { this.username = username; }

    public String getFullName()                  { return fullName; }
    public void setFullName(String fullName)     { this.fullName = fullName; }

    public String getRole()              { return role; }
    public void setRole(String role)     { this.role = role; }
}

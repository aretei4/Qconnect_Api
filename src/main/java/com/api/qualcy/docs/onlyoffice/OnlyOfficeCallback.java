package com.api.qualcy.docs.onlyoffice;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class OnlyOfficeCallback {
    private String key;
    private int status;
    private String url;
    private List<String> users;
    private List<Action> actions;
    private String token; // Only present if "token" is enabled in OnlyOffice config

    // Getters and Setters
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public List<String> getUsers() { return users; }
    public void setUsers(List<String> users) { this.users = users; }
    
    public List<Action> getActions() { return actions; }
    public void setActions(List<Action> actions) { this.actions = actions; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    // Nested class for "actions"
    public static class Action {
        @JsonProperty("type")
        private int type;
        private String userid;

        // Getters and Setters
        public int getType() { return type; }
        public void setType(int type) { this.type = type; }
        
        public String getUserid() { return userid; }
        public void setUserid(String userid) { this.userid = userid; }
    }
}

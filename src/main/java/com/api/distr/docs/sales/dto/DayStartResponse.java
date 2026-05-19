package com.api.distr.docs.sales.dto;

public class DayStartResponse {
    private boolean success;
    private String  message;
    private String  startTime;      // "09:23 AM"
    private boolean alreadyStarted;

    public DayStartResponse() {}

    public DayStartResponse(boolean success, String message, String startTime, boolean alreadyStarted) {
        this.success        = success;
        this.message        = message;
        this.startTime      = startTime;
        this.alreadyStarted = alreadyStarted;
    }

    public boolean isSuccess()        { return success; }
    public void    setSuccess(boolean v)        { this.success = v; }
    public String  getMessage()       { return message; }
    public void    setMessage(String v)         { this.message = v; }
    public String  getStartTime()     { return startTime; }
    public void    setStartTime(String v)       { this.startTime = v; }
    public boolean isAlreadyStarted() { return alreadyStarted; }
    public void    setAlreadyStarted(boolean v) { this.alreadyStarted = v; }
}

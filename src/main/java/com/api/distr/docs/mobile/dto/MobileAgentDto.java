package com.api.distr.docs.mobile.dto;

/** Step 1 — an open DAN awaiting close, one card per agent. */
public class MobileAgentDto {

    private String agentId;
    private String agentName;
    private Long   danId;
    private String danCode;
    private String date;      // dd-MM-yyyy

    public MobileAgentDto() {}

    public MobileAgentDto(String agentId, String agentName, Long danId, String danCode, String date) {
        this.agentId   = agentId;
        this.agentName = agentName;
        this.danId     = danId;
        this.danCode   = danCode;
        this.date      = date;
    }

    public String getAgentId()             { return agentId; }
    public void   setAgentId(String v)     { this.agentId = v; }

    public String getAgentName()           { return agentName; }
    public void   setAgentName(String v)   { this.agentName = v; }

    public Long   getDanId()               { return danId; }
    public void   setDanId(Long v)         { this.danId = v; }

    public String getDanCode()             { return danCode; }
    public void   setDanCode(String v)     { this.danCode = v; }

    public String getDate()                { return date; }
    public void   setDate(String v)        { this.date = v; }
}

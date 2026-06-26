package com.api.distr.docs.dan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;

public class DanListDto {

    private Long   danId;
    private String danCode;   // e.g. DAN-20260603-018

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate date;

    private String agentId;
    private String agentName;
    private String agentCode; // e.g. #50

    private List<DanPicklistDto> picklists;

    public Long   getDanId()                     { return danId; }
    public void   setDanId(Long v)               { this.danId = v; }

    public String getDanCode()                   { return danCode; }
    public void   setDanCode(String v)           { this.danCode = v; }

    public LocalDate getDate()                   { return date; }
    public void   setDate(LocalDate v)           { this.date = v; }

    public String getAgentId()                   { return agentId; }
    public void   setAgentId(String v)           { this.agentId = v; }

    public String getAgentName()                 { return agentName; }
    public void   setAgentName(String v)         { this.agentName = v; }

    public String getAgentCode()                 { return agentCode; }
    public void   setAgentCode(String v)         { this.agentCode = v; }

    public List<DanPicklistDto> getPicklists()           { return picklists; }
    public void   setPicklists(List<DanPicklistDto> v)   { this.picklists = v; }
}

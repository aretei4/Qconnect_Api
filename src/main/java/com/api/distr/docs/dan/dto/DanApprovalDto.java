package com.api.distr.docs.dan.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** One recorded approval/rejection event in a DAN's approval trail. */
public class DanApprovalDto {

    private Long   id;
    private Long   danId;
    private String stage;           // STOREKEEPER | ACCOUNTS
    private String action;          // APPROVED | REJECTED
    private String approvedBy;      // username
    private String approvedByRole;  // ADMIN | MANAGER | STAFF
    private String remarks;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime createdAt;

    public Long   getId()                       { return id; }
    public void   setId(Long v)                 { this.id = v; }

    public Long   getDanId()                    { return danId; }
    public void   setDanId(Long v)              { this.danId = v; }

    public String getStage()                    { return stage; }
    public void   setStage(String v)            { this.stage = v; }

    public String getAction()                   { return action; }
    public void   setAction(String v)           { this.action = v; }

    public String getApprovedBy()               { return approvedBy; }
    public void   setApprovedBy(String v)       { this.approvedBy = v; }

    public String getApprovedByRole()           { return approvedByRole; }
    public void   setApprovedByRole(String v)   { this.approvedByRole = v; }

    public String getRemarks()                  { return remarks; }
    public void   setRemarks(String v)          { this.remarks = v; }

    public LocalDateTime getCreatedAt()         { return createdAt; }
    public void   setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}

package com.api.distr.docs.dan.dto;

/**
 * Body of POST /api/dan/{danId}/approval
 *
 * { "stage": "STOREKEEPER", "action": "APPROVED", "approvedBy": "ravi", "remarks": "stock verified" }
 *
 * approvedBy is optional — when omitted the authenticated user is recorded.
 */
public class DanApprovalRequest {

    private String stage;        // STOREKEEPER | ACCOUNTS
    private String action;       // APPROVED | REJECTED  (default APPROVED)
    private String approvedBy;
    private String approvedByRole;
    private String remarks;

    public String getStage()                  { return stage; }
    public void   setStage(String v)          { this.stage = v; }

    public String getAction()                 { return action; }
    public void   setAction(String v)         { this.action = v; }

    public String getApprovedBy()             { return approvedBy; }
    public void   setApprovedBy(String v)     { this.approvedBy = v; }

    public String getApprovedByRole()         { return approvedByRole; }
    public void   setApprovedByRole(String v) { this.approvedByRole = v; }

    public String getRemarks()                { return remarks; }
    public void   setRemarks(String v)        { this.remarks = v; }
}

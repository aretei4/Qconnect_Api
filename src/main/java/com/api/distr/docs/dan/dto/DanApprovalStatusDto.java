package com.api.distr.docs.dan.dto;

import java.util.ArrayList;
import java.util.List;

/** Current approval state of a DAN plus its full audit trail. */
public class DanApprovalStatusDto {

    private Long    danId;
    private String  danStatus;            // dayend_approval.status
    private boolean storekeeperApproved;
    private String  storekeeperBy;
    private String  storekeeperAt;        // dd/MM/yyyy HH:mm:ss
    private boolean accountsApproved;
    private String  accountsBy;
    private String  accountsAt;
    private boolean fullyApproved;
    private String  pendingStage;         // next stage awaiting action, null when done
    private List<DanApprovalDto> trail = new ArrayList<>();

    public Long    getDanId()                       { return danId; }
    public void    setDanId(Long v)                 { this.danId = v; }

    public String  getDanStatus()                   { return danStatus; }
    public void    setDanStatus(String v)           { this.danStatus = v; }

    public boolean isStorekeeperApproved()          { return storekeeperApproved; }
    public void    setStorekeeperApproved(boolean v){ this.storekeeperApproved = v; }

    public String  getStorekeeperBy()               { return storekeeperBy; }
    public void    setStorekeeperBy(String v)       { this.storekeeperBy = v; }

    public String  getStorekeeperAt()               { return storekeeperAt; }
    public void    setStorekeeperAt(String v)       { this.storekeeperAt = v; }

    public boolean isAccountsApproved()             { return accountsApproved; }
    public void    setAccountsApproved(boolean v)   { this.accountsApproved = v; }

    public String  getAccountsBy()                  { return accountsBy; }
    public void    setAccountsBy(String v)          { this.accountsBy = v; }

    public String  getAccountsAt()                  { return accountsAt; }
    public void    setAccountsAt(String v)          { this.accountsAt = v; }

    public boolean isFullyApproved()                { return fullyApproved; }
    public void    setFullyApproved(boolean v)      { this.fullyApproved = v; }

    public String  getPendingStage()                { return pendingStage; }
    public void    setPendingStage(String v)        { this.pendingStage = v; }

    public List<DanApprovalDto> getTrail()          { return trail; }
    public void    setTrail(List<DanApprovalDto> v) { this.trail = v; }
}

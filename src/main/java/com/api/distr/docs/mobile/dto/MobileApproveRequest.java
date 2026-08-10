package com.api.distr.docs.mobile.dto;

import java.util.HashMap;
import java.util.Map;

/** Body of POST /api/mobile/dan/{danId}/approve */
public class MobileApproveRequest {

    /** cash / upi / cheque / neft → amount */
    private Map<String, Double> payments = new HashMap<>();

    /** rowId → { billQty, billAmt, retQty, retAmt } */
    private Map<String, Map<String, Double>> returns = new HashMap<>();

    private String agentId;

    /** Who is signing off this stage — falls back to the authenticated user. */
    private String approvedBy;
    private String remarks;

    public Map<String, Double> getPayments()                    { return payments; }
    public void setPayments(Map<String, Double> v)              { this.payments = v; }

    public Map<String, Map<String, Double>> getReturns()        { return returns; }
    public void setReturns(Map<String, Map<String, Double>> v)  { this.returns = v; }

    public String getAgentId()                                  { return agentId; }
    public void   setAgentId(String v)                          { this.agentId = v; }

    public String getApprovedBy()                               { return approvedBy; }
    public void   setApprovedBy(String v)                       { this.approvedBy = v; }

    public String getRemarks()                                  { return remarks; }
    public void   setRemarks(String v)                          { this.remarks = v; }
}

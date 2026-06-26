package com.api.distr.docs.dan.dto;

/**
 * One row returned by GET /api/dan/pamt-check
 * Represents a partial return where only an amount was recorded (nd_type = 'pamt').
 * Full return details are still missing and should be completed by the delivery agent.
 */
public class PamtReturnItem {

    private long   direId;
    private double returnAmt;
    private String custDesc;
    private String invoiceNo;
    private String picklistNo;
    private String customerNo;
    private double netValue;

    public PamtReturnItem() {}

    public PamtReturnItem(long direId, double returnAmt, String custDesc,
                          String invoiceNo, String picklistNo,
                          String customerNo, double netValue) {
        this.direId      = direId;
        this.returnAmt   = returnAmt;
        this.custDesc    = custDesc;
        this.invoiceNo   = invoiceNo;
        this.picklistNo  = picklistNo;
        this.customerNo  = customerNo;
        this.netValue    = netValue;
    }

    public long   getDireId()     { return direId; }
    public double getReturnAmt()  { return returnAmt; }
    public String getCustDesc()   { return custDesc; }
    public String getInvoiceNo()  { return invoiceNo; }
    public String getPicklistNo() { return picklistNo; }
    public String getCustomerNo() { return customerNo; }
    public double getNetValue()   { return netValue; }

    public void setDireId(long v)      { this.direId     = v; }
    public void setReturnAmt(double v) { this.returnAmt  = v; }
    public void setCustDesc(String v)  { this.custDesc   = v; }
    public void setInvoiceNo(String v) { this.invoiceNo  = v; }
    public void setPicklistNo(String v){ this.picklistNo = v; }
    public void setCustomerNo(String v){ this.customerNo = v; }
    public void setNetValue(double v)  { this.netValue   = v; }
}

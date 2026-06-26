package com.api.distr.docs.sales.dto;

import java.sql.Date;

public class SalesEntryDto {

    private Long    direId;
    private String  invoiceNo;      // stage_sales_entery.sales_order_no
    private String  picklistNo;
    private String  customerNo;
    private String  custDesc;
    private String  custMobile;
    private String  billingDate;
    private String  netValue;
    private String  updateDate;

    // ── payment / delivery fields (populated by day-end picklist query) ────────
    private boolean delivered;
    private double  paymentAmount;
    private String  paymentMode;
    private String  reason;
    private int     assignStatus;   // 0=PENDING, 1=FAILED, 2=DELIVERED

    public Long    getDireId()     { return direId; }
    public void    setDireId(Long direId)           { this.direId = direId; }

    public String  getInvoiceNo()   { return invoiceNo; }
    public void    setInvoiceNo(String invoiceNo)     { this.invoiceNo = invoiceNo; }

    public String  getPicklistNo()  { return picklistNo; }
    public void    setPicklistNo(String picklistNo)   { this.picklistNo = picklistNo; }

    public String  getCustomerNo()  { return customerNo; }
    public void    setCustomerNo(String customerNo)  { this.customerNo = customerNo; }

    public String  getCustDesc()    { return custDesc; }
    public void    setCustDesc(String custDesc)      { this.custDesc = custDesc; }

    public String  getCustMobile()  { return custMobile; }
    public void    setCustMobile(String custMobile)  { this.custMobile = custMobile; }

    public String  getBillingDate() { return billingDate; }
    public void    setBillingDate(String billingDate){ this.billingDate = billingDate; }

    public String  getNetValue()    { return netValue; }
    public void    setNetValue(String netValue)      { this.netValue = netValue; }

    public String  getUpdateDate()  { return updateDate; }
    public void    setUpdateDate(String updateDate)  { this.updateDate = updateDate; }

    public boolean isDelivered()    { return delivered; }
    public void    setDelivered(boolean delivered)   { this.delivered = delivered; }

    public double  getPaymentAmount(){ return paymentAmount; }
    public void    setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }

    public String  getPaymentMode() { return paymentMode; }
    public void    setPaymentMode(String paymentMode){ this.paymentMode = paymentMode; }

    public String  getReason()      { return reason; }
    public void    setReason(String reason)          { this.reason = reason; }

    public int     getAssignStatus(){ return assignStatus; }
    public void    setAssignStatus(int assignStatus) { this.assignStatus = assignStatus; }
}


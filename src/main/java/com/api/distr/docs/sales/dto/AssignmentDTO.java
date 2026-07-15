package com.api.distr.docs.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AssignmentDTO {

    @JsonProperty("direId")       private Long   direId;
    @JsonProperty("invoiceNo")    private String invoiceNo;
    @JsonProperty("customerNo")   private String customerNo;
    @JsonProperty("custDesc")     private String custDesc;
    @JsonProperty("custMobile")   private String custMobile;
    @JsonProperty("netValue")     private String netValue;
    @JsonProperty("billingDate")  private String billingDate;
    @JsonProperty("deliveryDate") private String deliveryDate;
    @JsonProperty("agentId")      private String agentId;
    @JsonProperty("agentName")    private String agentName;
    @JsonProperty("assignStatus") private int    assignStatus;
    @JsonProperty("statusLabel")  private String statusLabel;

    public Long   getDireId()       { return direId; }
    public void   setDireId(Long v)         { this.direId = v; }

    public String getInvoiceNo()    { return invoiceNo; }
    public void   setInvoiceNo(String v)    { this.invoiceNo = v; }

    public String getCustomerNo()   { return customerNo; }
    public void   setCustomerNo(String v)   { this.customerNo = v; }

    public String getCustDesc()     { return custDesc; }
    public void   setCustDesc(String v)     { this.custDesc = v; }

    public String getCustMobile()   { return custMobile; }
    public void   setCustMobile(String v)   { this.custMobile = v; }

    public String getNetValue()     { return netValue; }
    public void   setNetValue(String v)     { this.netValue = v; }

    public String getBillingDate()  { return billingDate; }
    public void   setBillingDate(String v)  { this.billingDate = v; }

    public String getDeliveryDate() { return deliveryDate; }
    public void   setDeliveryDate(String v) { this.deliveryDate = v; }

    public String getAgentId()      { return agentId; }
    public void   setAgentId(String v)      { this.agentId = v; }

    public String getAgentName()    { return agentName; }
    public void   setAgentName(String v)    { this.agentName = v; }

    public int    getAssignStatus() { return assignStatus; }
    public void   setAssignStatus(int v)    { this.assignStatus = v; }

    public String getStatusLabel()  { return statusLabel; }
    public void   setStatusLabel(String v)  { this.statusLabel = v; }
}

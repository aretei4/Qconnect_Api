package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PaymentModeEntry;

import java.util.List;

public class DayEndPicklistDetail {

    private Long   direId;
    private String invoiceNo;       // stage_sales_entery.sales_order_no
    private String picklistNo;
    private String custDesc;
    private boolean delivered;
    private double netValue;
    private double paymentAmount;
    private List<PaymentModeEntry> paymentModes;
    private String reason;

    public Long   getDireId()                          { return direId; }
    public void   setDireId(Long direId)             { this.direId = direId; }

    public String getInvoiceNo()                       { return invoiceNo; }
    public void   setInvoiceNo(String invoiceNo)       { this.invoiceNo = invoiceNo; }

    public String getPicklistNo()                      { return picklistNo; }
    public void   setPicklistNo(String picklistNo)     { this.picklistNo = picklistNo; }

    public String getCustDesc()                        { return custDesc; }
    public void setCustDesc(String custDesc)           { this.custDesc = custDesc; }

    public boolean isDelivered()                       { return delivered; }
    public void setDelivered(boolean delivered)        { this.delivered = delivered; }

    public double getNetValue()                        { return netValue; }
    public void setNetValue(double netValue)           { this.netValue = netValue; }

    public double getPaymentAmount()                   { return paymentAmount; }
    public void setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }

    public List<PaymentModeEntry> getPaymentModes()            { return paymentModes; }
    public void setPaymentModes(List<PaymentModeEntry> modes)  { this.paymentModes = modes; }

    public String getReason()                          { return reason; }
    public void setReason(String reason)               { this.reason = reason; }
}

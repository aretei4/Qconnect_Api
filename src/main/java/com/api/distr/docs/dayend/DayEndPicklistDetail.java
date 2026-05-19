package com.api.distr.docs.dayend;

import com.api.distr.docs.sales.dto.PaymentModeEntry;

import java.util.List;

public class DayEndPicklistDetail {

    private String picklistNo;
    private String custDesc;
    private boolean delivered;
    private double netValue;
    private double paymentAmount;
    private List<PaymentModeEntry> paymentModes;
    private String reason;

    public String getPicklistNo()                      { return picklistNo; }
    public void setPicklistNo(String picklistNo)       { this.picklistNo = picklistNo; }

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

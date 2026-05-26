package com.api.distr.docs.sales.dto;

/**
 * Request body for PUT /api/delivery/picklist/{picklistNo}
 * Used by the Day-End screen to correct payment details.
 */
public class PicklistUpdateRequest {

    private boolean delivered;
    private double  paymentAmount;
    private String  paymentMode;   // e.g. "CASH:500.0,UPI:300.0"
    private String  reason;

    public boolean isDelivered()      { return delivered; }
    public void    setDelivered(boolean delivered)        { this.delivered = delivered; }

    public double  getPaymentAmount() { return paymentAmount; }
    public void    setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }

    public String  getPaymentMode()   { return paymentMode; }
    public void    setPaymentMode(String paymentMode)     { this.paymentMode = paymentMode; }

    public String  getReason()        { return reason; }
    public void    setReason(String reason)               { this.reason = reason; }
}

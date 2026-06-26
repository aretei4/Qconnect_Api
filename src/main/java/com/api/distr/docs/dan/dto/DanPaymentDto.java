package com.api.distr.docs.dan.dto;

/**
 * Payment settlement for one picklist in the DAN close flow.
 * payment_mode is stored as a JSON array string identical to delivery_status.payment_mode.
 * e.g. [{"mode":"CASH","amount":5000},{"mode":"UPI","amount":3000,"referenceNo":"UTR123"}]
 */
public class DanPaymentDto {
    private boolean delivered;
    private double  paymentAmount;
    private String  paymentMode;   // JSON array string
    private String  reason;

    public boolean isDelivered()             { return delivered; }
    public void    setDelivered(boolean v)   { this.delivered = v; }

    public double  getPaymentAmount()        { return paymentAmount; }
    public void    setPaymentAmount(double v){ this.paymentAmount = v; }

    public String  getPaymentMode()          { return paymentMode; }
    public void    setPaymentMode(String v)  { this.paymentMode = v; }

    public String  getReason()               { return reason; }
    public void    setReason(String v)       { this.reason = v; }
}

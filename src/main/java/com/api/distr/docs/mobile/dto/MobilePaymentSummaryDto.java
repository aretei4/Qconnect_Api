package com.api.distr.docs.mobile.dto;

/**
 * Step 3 — pre-filled payment amounts plus the static totals.
 * The client computes: totalCollected = cash+upi+cheque+neft, credit = netValue − totalCollected.
 */
public class MobilePaymentSummaryDto {

    private double cash;
    private double upi;
    private double cheque;
    private double neft;
    private double netValue;
    private double returnAmt;

    public MobilePaymentSummaryDto() {}

    public MobilePaymentSummaryDto(double cash, double upi, double cheque,
                                   double neft, double netValue, double returnAmt) {
        this.cash      = cash;
        this.upi       = upi;
        this.cheque    = cheque;
        this.neft      = neft;
        this.netValue  = netValue;
        this.returnAmt = returnAmt;
    }

    public double getCash()               { return cash; }
    public void   setCash(double v)       { this.cash = v; }

    public double getUpi()                { return upi; }
    public void   setUpi(double v)        { this.upi = v; }

    public double getCheque()             { return cheque; }
    public void   setCheque(double v)     { this.cheque = v; }

    public double getNeft()               { return neft; }
    public void   setNeft(double v)       { this.neft = v; }

    public double getNetValue()           { return netValue; }
    public void   setNetValue(double v)   { this.netValue = v; }

    public double getReturnAmt()          { return returnAmt; }
    public void   setReturnAmt(double v)  { this.returnAmt = v; }
}

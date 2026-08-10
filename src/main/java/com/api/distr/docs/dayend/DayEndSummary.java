package com.api.distr.docs.dayend;

import java.util.List;
import java.util.Map;

public class DayEndSummary {

    private long total;
    private long delivered;
    private long failed;
    /** SUM(payment_details.total_amount) — includes credit. */
    private double totalAmount;

    /** SUM(payment_details.credit_amount). */
    private double creditAmount;

    /** SUM(payment_details.return_amount). */
    private double returnAmount;

    /** totalAmount − creditAmount, i.e. what was actually collected. */
    private double collectedAmount;

    private Map<String, Double> amountByPaymentMode;

    /** Per-picklist details with individual payment breakdown */
    private List<DayEndPicklistDetail> picklists;

    public long getTotal()                              { return total; }
    public void setTotal(long total)                    { this.total = total; }

    public long getDelivered()                          { return delivered; }
    public void setDelivered(long delivered)            { this.delivered = delivered; }

    public long getFailed()                             { return failed; }
    public void setFailed(long failed)                  { this.failed = failed; }

    public double getTotalAmount()                      { return totalAmount; }
    public void setTotalAmount(double totalAmount)      { this.totalAmount = totalAmount; }

    public double getCreditAmount()                     { return creditAmount; }
    public void setCreditAmount(double creditAmount)    { this.creditAmount = creditAmount; }

    public double getReturnAmount()                     { return returnAmount; }
    public void setReturnAmount(double returnAmount)    { this.returnAmount = returnAmount; }

    public double getCollectedAmount()                      { return collectedAmount; }
    public void setCollectedAmount(double collectedAmount)  { this.collectedAmount = collectedAmount; }

    public Map<String, Double> getAmountByPaymentMode()                      { return amountByPaymentMode; }
    public void setAmountByPaymentMode(Map<String, Double> amountByPaymentMode) { this.amountByPaymentMode = amountByPaymentMode; }

    public List<DayEndPicklistDetail> getPicklists()             { return picklists; }
    public void setPicklists(List<DayEndPicklistDetail> picklists) { this.picklists = picklists; }
}

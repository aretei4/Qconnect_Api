package com.api.distr.docs.dayend;

import java.util.List;
import java.util.Map;

public class DayEndSummary {

    private long total;
    private long delivered;
    private long failed;
    private double totalAmount;
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

    public Map<String, Double> getAmountByPaymentMode()                      { return amountByPaymentMode; }
    public void setAmountByPaymentMode(Map<String, Double> amountByPaymentMode) { this.amountByPaymentMode = amountByPaymentMode; }

    public List<DayEndPicklistDetail> getPicklists()             { return picklists; }
    public void setPicklists(List<DayEndPicklistDetail> picklists) { this.picklists = picklists; }
}

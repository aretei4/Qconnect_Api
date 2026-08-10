package com.api.distr.docs.dashboard;

import java.util.ArrayList;
import java.util.List;

/** Overall summary report — built from payment_details + stage_sales_entery. */
public class OverallReportDto {

    public static class CreditStore {
        private String name;
        private double amount;

        public CreditStore() {}
        public CreditStore(String name, double amount) { this.name = name; this.amount = amount; }

        public String getName()           { return name; }
        public void   setName(String v)   { this.name = v; }
        public double getAmount()         { return amount; }
        public void   setAmount(double v) { this.amount = v; }
    }

    private int    totalOrders;
    private double totalNetValue;
    private double totalCollected;
    private double outstandingCredit;
    private int    pendingStores;

    private double cashAmount;
    private double upiAmount;
    private double chequeAmount;
    private double neftAmount;
    private double creditAmount;

    private List<CreditStore> topCreditStores = new ArrayList<>();

    public int    getTotalOrders()                     { return totalOrders; }
    public void   setTotalOrders(int v)                { this.totalOrders = v; }
    public double getTotalNetValue()                   { return totalNetValue; }
    public void   setTotalNetValue(double v)           { this.totalNetValue = v; }
    public double getTotalCollected()                  { return totalCollected; }
    public void   setTotalCollected(double v)          { this.totalCollected = v; }
    public double getOutstandingCredit()               { return outstandingCredit; }
    public void   setOutstandingCredit(double v)       { this.outstandingCredit = v; }
    public int    getPendingStores()                   { return pendingStores; }
    public void   setPendingStores(int v)              { this.pendingStores = v; }

    public double getCashAmount()                      { return cashAmount; }
    public void   setCashAmount(double v)              { this.cashAmount = v; }
    public double getUpiAmount()                       { return upiAmount; }
    public void   setUpiAmount(double v)               { this.upiAmount = v; }
    public double getChequeAmount()                    { return chequeAmount; }
    public void   setChequeAmount(double v)            { this.chequeAmount = v; }
    public double getNeftAmount()                      { return neftAmount; }
    public void   setNeftAmount(double v)              { this.neftAmount = v; }
    public double getCreditAmount()                    { return creditAmount; }
    public void   setCreditAmount(double v)            { this.creditAmount = v; }

    public List<CreditStore> getTopCreditStores()        { return topCreditStores; }
    public void setTopCreditStores(List<CreditStore> v)  { this.topCreditStores = v; }
}

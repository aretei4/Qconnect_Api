package com.api.distr.docs.dan.dto;

import java.util.ArrayList;
import java.util.List;

/** Detail screen of a single DAN in the Close Report. */
public class DanReportDetailDto {

    public static class PaymentEntry {
        private String mode;
        private double amount;
        private String chequeNo;
        private String bankName;
        private String referenceNo;

        public PaymentEntry() {}
        public PaymentEntry(String mode, double amount) { this.mode = mode; this.amount = amount; }

        public String getMode()                { return mode; }
        public void   setMode(String v)        { this.mode = v; }
        public double getAmount()              { return amount; }
        public void   setAmount(double v)      { this.amount = v; }
        public String getChequeNo()            { return chequeNo; }
        public void   setChequeNo(String v)    { this.chequeNo = v; }
        public String getBankName()            { return bankName; }
        public void   setBankName(String v)    { this.bankName = v; }
        public String getReferenceNo()         { return referenceNo; }
        public void   setReferenceNo(String v) { this.referenceNo = v; }
    }

    public static class ReturnItem {
        private String description;
        private int    qty;
        private double amount;
        private String reason;

        public String getDescription()          { return description; }
        public void   setDescription(String v)  { this.description = v; }
        public int    getQty()                  { return qty; }
        public void   setQty(int v)             { this.qty = v; }
        public double getAmount()               { return amount; }
        public void   setAmount(double v)       { this.amount = v; }
        public String getReason()               { return reason; }
        public void   setReason(String v)       { this.reason = v; }
    }

    public static class InvoiceRow {
        private long   direId;
        private String invoiceNo;
        private String custName;
        private double amount;
        private double returnAmt;
        private double paidAmount;
        private List<PaymentEntry> payments = new ArrayList<>();
        private List<ReturnItem>   returns  = new ArrayList<>();

        public long   getDireId()                       { return direId; }
        public void   setDireId(long v)                 { this.direId = v; }
        public String getInvoiceNo()                    { return invoiceNo; }
        public void   setInvoiceNo(String v)            { this.invoiceNo = v; }
        public String getCustName()                     { return custName; }
        public void   setCustName(String v)             { this.custName = v; }
        public double getAmount()                       { return amount; }
        public void   setAmount(double v)               { this.amount = v; }
        public double getReturnAmt()                    { return returnAmt; }
        public void   setReturnAmt(double v)            { this.returnAmt = v; }
        public double getPaidAmount()                   { return paidAmount; }
        public void   setPaidAmount(double v)           { this.paidAmount = v; }
        public List<PaymentEntry> getPayments()         { return payments; }
        public void   setPayments(List<PaymentEntry> v) { this.payments = v; }
        public List<ReturnItem> getReturns()            { return returns; }
        public void   setReturns(List<ReturnItem> v)    { this.returns = v; }
    }

    private long   danId;
    private String danCode;
    private String date;
    private String agentName;
    private String agentCode;
    private String status;
    private double totalAmount;
    private double netSettled;
    private int    deliveries;
    private int    returnsCount;
    private double returnsAmt;
    private List<InvoiceRow> invoices = new ArrayList<>();

    public long   getDanId()                     { return danId; }
    public void   setDanId(long v)               { this.danId = v; }
    public String getDanCode()                   { return danCode; }
    public void   setDanCode(String v)           { this.danCode = v; }
    public String getDate()                      { return date; }
    public void   setDate(String v)              { this.date = v; }
    public String getAgentName()                 { return agentName; }
    public void   setAgentName(String v)         { this.agentName = v; }
    public String getAgentCode()                 { return agentCode; }
    public void   setAgentCode(String v)         { this.agentCode = v; }
    public String getStatus()                    { return status; }
    public void   setStatus(String v)            { this.status = v; }
    public double getTotalAmount()               { return totalAmount; }
    public void   setTotalAmount(double v)       { this.totalAmount = v; }
    public double getNetSettled()                { return netSettled; }
    public void   setNetSettled(double v)        { this.netSettled = v; }
    public int    getDeliveries()                { return deliveries; }
    public void   setDeliveries(int v)           { this.deliveries = v; }
    public int    getReturnsCount()              { return returnsCount; }
    public void   setReturnsCount(int v)         { this.returnsCount = v; }
    public double getReturnsAmt()                { return returnsAmt; }
    public void   setReturnsAmt(double v)        { this.returnsAmt = v; }
    public List<InvoiceRow> getInvoices()        { return invoices; }
    public void   setInvoices(List<InvoiceRow> v){ this.invoices = v; }
}

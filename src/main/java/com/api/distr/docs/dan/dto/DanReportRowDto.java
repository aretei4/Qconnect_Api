package com.api.distr.docs.dan.dto;

/** One row of the DAN Close Report list. */
public class DanReportRowDto {

    private long   danId;
    private String danCode;
    private String date;        // yyyy-MM-dd
    private String agentName;
    private String agentCode;
    private int    deliveries;
    private double amount;
    private double returnsAmt;
    private String status;      // Closed | Pending

    public long   getDanId()               { return danId; }
    public void   setDanId(long v)         { this.danId = v; }

    public String getDanCode()             { return danCode; }
    public void   setDanCode(String v)     { this.danCode = v; }

    public String getDate()                { return date; }
    public void   setDate(String v)        { this.date = v; }

    public String getAgentName()           { return agentName; }
    public void   setAgentName(String v)   { this.agentName = v; }

    public String getAgentCode()           { return agentCode; }
    public void   setAgentCode(String v)   { this.agentCode = v; }

    public int    getDeliveries()          { return deliveries; }
    public void   setDeliveries(int v)     { this.deliveries = v; }

    public double getAmount()              { return amount; }
    public void   setAmount(double v)      { this.amount = v; }

    public double getReturnsAmt()          { return returnsAmt; }
    public void   setReturnsAmt(double v)  { this.returnsAmt = v; }

    public String getStatus()              { return status; }
    public void   setStatus(String v)      { this.status = v; }
}

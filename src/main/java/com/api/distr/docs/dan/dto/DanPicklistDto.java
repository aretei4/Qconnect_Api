package com.api.distr.docs.dan.dto;

public class DanPicklistDto {
    private int    no;
    private Long   direId;     // stage_sales_entery.dire_id — unique transaction reference
    private String invoiceNo;   // stage_sales_entery.sales_order_no
    private String picklistNo;
    private String custName;
    private String custNo;
    private double netValue;

    public int    getNo()              { return no; }
    public void   setNo(int v)         { this.no = v; }

    public Long   getDireId()         { return direId; }
    public void   setDireId(Long v)   { this.direId = v; }

    public String getInvoiceNo()       { return invoiceNo; }
    public void   setInvoiceNo(String v) { this.invoiceNo = v; }

    public String getPicklistNo()      { return picklistNo; }
    public void   setPicklistNo(String v) { this.picklistNo = v; }

    public String getCustName()        { return custName; }
    public void   setCustName(String v){ this.custName = v; }

    public String getCustNo()          { return custNo; }
    public void   setCustNo(String v)  { this.custNo = v; }

    public double getNetValue()        { return netValue; }
    public void   setNetValue(double v){ this.netValue = v; }
}

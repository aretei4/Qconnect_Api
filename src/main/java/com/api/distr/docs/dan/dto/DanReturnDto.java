package com.api.distr.docs.dan.dto;

public class DanReturnDto {
    private Long    id;
    private Long    direId;
    private String  picklistNo;
    private String  serial;
    private String  description;
    private int     billQty;
    private double  billAmt;
    private int     returnQty;
    private double  returnAmt;
    private String  reason;
    private boolean isCustom;

    public Long   getId()                  { return id; }
    public void   setId(Long v)            { this.id = v; }

    public Long   getDireId()              { return direId; }
    public void   setDireId(Long v)        { this.direId = v; }

    public String getPicklistNo()          { return picklistNo; }
    public void   setPicklistNo(String v)  { this.picklistNo = v; }

    public String getSerial()              { return serial; }
    public void   setSerial(String v)      { this.serial = v; }

    public String getDescription()         { return description; }
    public void   setDescription(String v) { this.description = v; }

    public int    getBillQty()             { return billQty; }
    public void   setBillQty(int v)        { this.billQty = v; }

    public double getBillAmt()             { return billAmt; }
    public void   setBillAmt(double v)     { this.billAmt = v; }

    public int    getReturnQty()           { return returnQty; }
    public void   setReturnQty(int v)      { this.returnQty = v; }

    public double getReturnAmt()           { return returnAmt; }
    public void   setReturnAmt(double v)   { this.returnAmt = v; }

    public String getReason()              { return reason; }
    public void   setReason(String v)      { this.reason = v; }

    public boolean isCustom()              { return isCustom; }
    public void   setCustom(boolean v)     { this.isCustom = v; }
}

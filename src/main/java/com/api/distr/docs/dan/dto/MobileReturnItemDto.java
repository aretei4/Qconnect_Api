package com.api.distr.docs.dan.dto;

/** One return-item row as sent by the Android mobile app. */
public class MobileReturnItemDto {

    private String serial;
    private String desc;          // Android uses "desc"; mapped to DanReturnDto.description
    private double billQty;
    private double billAmt;
    private double returnQty;
    private double returnAmt;
    private String reason;

    public String getSerial()               { return serial; }
    public void   setSerial(String v)       { this.serial = v; }

    public String getDesc()                 { return desc; }
    public void   setDesc(String v)         { this.desc = v; }

    public double getBillQty()              { return billQty; }
    public void   setBillQty(double v)      { this.billQty = v; }

    public double getBillAmt()              { return billAmt; }
    public void   setBillAmt(double v)      { this.billAmt = v; }

    public double getReturnQty()            { return returnQty; }
    public void   setReturnQty(double v)    { this.returnQty = v; }

    public double getReturnAmt()            { return returnAmt; }
    public void   setReturnAmt(double v)    { this.returnAmt = v; }

    public String getReason()               { return reason; }
    public void   setReason(String v)       { this.reason = v; }
}

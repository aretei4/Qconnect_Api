package com.api.distr.docs.dan.dto;

/**
 * One return-item row as posted by the web frontend (DanClosePage).
 * Matches the shape: { serial, description, billQty, billAmt, returnQty, returnAmt, reason, custom }
 */
public class WebReturnItemDto {

    private String  serial;
    private String  description;
    private double  billQty;
    private double  billAmt;
    private double  returnQty;
    private double  returnAmt;
    private String  reason;
    private boolean custom;

    public String  getSerial()                { return serial; }
    public void    setSerial(String v)        { this.serial = v; }

    public String  getDescription()           { return description; }
    public void    setDescription(String v)   { this.description = v; }

    public double  getBillQty()               { return billQty; }
    public void    setBillQty(double v)       { this.billQty = v; }

    public double  getBillAmt()               { return billAmt; }
    public void    setBillAmt(double v)       { this.billAmt = v; }

    public double  getReturnQty()             { return returnQty; }
    public void    setReturnQty(double v)     { this.returnQty = v; }

    public double  getReturnAmt()             { return returnAmt; }
    public void    setReturnAmt(double v)     { this.returnAmt = v; }

    public String  getReason()                { return reason; }
    public void    setReason(String v)        { this.reason = v; }

    public boolean isCustom()                 { return custom; }
    public void    setCustom(boolean v)       { this.custom = v; }
}

package com.api.distr.docs.upload;

import java.sql.Date;

public class DeliveryExcelDTO {

    // ── Core ──────────────────────────────────────────────────────────────────
    private String deliveryName;
    private String deliveryMobile;
    private String altMobile;
    private Date   updatedDate;
    private Double lat;
    private Double lon;
    private String type;

    // ── Address ───────────────────────────────────────────────────────────────
    private String address;       // legacy column kept for backward compat
    private String address1;
    private String address2;
    private String address3;
    private String city;
    private String pinCode;

    // ── Identity & Banking ────────────────────────────────────────────────────
    private String fatherName;
    private String aadharNo;
    private String panCard;
    private String bankAccount;
    private Date   dateOfJoining;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getDeliveryName()                        { return deliveryName; }
    public void   setDeliveryName(String deliveryName)     { this.deliveryName = deliveryName; }

    public String getDeliveryMobile()                      { return deliveryMobile; }
    public void   setDeliveryMobile(String deliveryMobile) { this.deliveryMobile = deliveryMobile; }

    public String getAltMobile()                           { return altMobile; }
    public void   setAltMobile(String altMobile)           { this.altMobile = altMobile; }

    public Date   getUpdatedDate()                         { return updatedDate; }
    public void   setUpdatedDate(Date updatedDate)         { this.updatedDate = updatedDate; }

    public Double getLat()                                 { return lat; }
    public void   setLat(Double lat)                       { this.lat = lat; }

    public Double getLon()                                 { return lon; }
    public void   setLon(Double lon)                       { this.lon = lon; }

    public String getType()                                { return type; }
    public void   setType(String type)                     { this.type = type; }

    public String getAddress()                             { return address; }
    public void   setAddress(String address)               { this.address = address; }

    public String getAddress1()                            { return address1; }
    public void   setAddress1(String address1)             { this.address1 = address1; }

    public String getAddress2()                            { return address2; }
    public void   setAddress2(String address2)             { this.address2 = address2; }

    public String getAddress3()                            { return address3; }
    public void   setAddress3(String address3)             { this.address3 = address3; }

    public String getCity()                                { return city; }
    public void   setCity(String city)                     { this.city = city; }

    public String getPinCode()                             { return pinCode; }
    public void   setPinCode(String pinCode)               { this.pinCode = pinCode; }

    public String getFatherName()                          { return fatherName; }
    public void   setFatherName(String fatherName)         { this.fatherName = fatherName; }

    public String getAadharNo()                            { return aadharNo; }
    public void   setAadharNo(String aadharNo)             { this.aadharNo = aadharNo; }

    public String getPanCard()                             { return panCard; }
    public void   setPanCard(String panCard)               { this.panCard = panCard; }

    public String getBankAccount()                         { return bankAccount; }
    public void   setBankAccount(String bankAccount)       { this.bankAccount = bankAccount; }

    public Date   getDateOfJoining()                       { return dateOfJoining; }
    public void   setDateOfJoining(Date dateOfJoining)     { this.dateOfJoining = dateOfJoining; }
}

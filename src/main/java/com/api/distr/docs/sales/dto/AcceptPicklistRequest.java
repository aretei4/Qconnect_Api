package com.api.distr.docs.sales.dto;

public class AcceptPicklistRequest {
    private Long   deliveryId;
    private String picklistNo;

    public AcceptPicklistRequest() {}

    public Long   getDeliveryId()  { return deliveryId; }
    public void   setDeliveryId(Long v)   { this.deliveryId = v; }
    public String getPicklistNo()  { return picklistNo; }
    public void   setPicklistNo(String v) { this.picklistNo = v; }
}

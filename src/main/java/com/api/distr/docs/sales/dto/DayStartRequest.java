package com.api.distr.docs.sales.dto;

public class DayStartRequest {
    private Long   deliveryId;
    private String date;   // "dd-MM-yyyy"
    private Double lat;
    private Double lon;

    public DayStartRequest() {}

    public Long   getDeliveryId() { return deliveryId; }
    public void   setDeliveryId(Long v)  { this.deliveryId = v; }
    public String getDate()       { return date; }
    public void   setDate(String v)      { this.date = v; }
    public Double getLat()        { return lat; }
    public void   setLat(Double v)       { this.lat = v; }
    public Double getLon()        { return lon; }
    public void   setLon(Double v)       { this.lon = v; }
}

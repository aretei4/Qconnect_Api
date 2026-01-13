package com.api.distr.docs.sales.dto;


public class DeliveryStatus {
    private Long delivery_id;
    private boolean delivered;
    private boolean otp;
    private double paymentAmount;
    private String paymentMode;
    private String picklistNo;
    private String reason;
    private Double lat;
    private Double lon;
    
    
    public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLon() {
		return lon;
	}
	public void setLon(Double lon) {
		this.lon = lon;
	}
	// Getters and setters
    public Long getDelivery_id() { return delivery_id; }
    public void setDelivery_id(Long delivery_id) { this.delivery_id = delivery_id; }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public boolean isOtp() { return otp; }
    public void setOtp(boolean otp) { this.otp = otp; }

    public double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getPicklistNo() { return picklistNo; }
    public void setPicklistNo(String picklistNo) { this.picklistNo = picklistNo; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

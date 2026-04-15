package com.api.distr.docs.dayend;

public class DayEndApprovalRequest {

    private String date; // dd-MM-yyyy
    private Long deliveryId;
    private Double totalAmount;
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public Long getDeliveryId() {
		return deliveryId;
	}
	public void setDeliveryId(Long deliveryId) {
		this.deliveryId = deliveryId;
	}
	public Double getTotalAmount() {
		return totalAmount;
	}
	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

    // getters & setters
}

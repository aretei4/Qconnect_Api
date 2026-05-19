package com.api.distr.docs.dayend;

public class DayEndDto {

    private String date;           // dd-MM-yyyy
    private Long deliveryId;
    private Double totalAmount;
    private java.util.List<String> picklistNos;  // picklists included in this day-end

    // for reject
    private String rejectReason;
    private Long dayendId;
	
	public Long getDayendId() {
		return dayendId;
	}

	public void setDayendId(Long dayendId) {
		this.dayendId = dayendId;
	}

	public java.util.List<String> getPicklistNos() { return picklistNos; }
	public void setPicklistNos(java.util.List<String> picklistNos) { this.picklistNos = picklistNos; }

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

	public String getRejectReason() {
		return rejectReason;
	}

	public void setRejectReason(String rejectReason) {
		this.rejectReason = rejectReason;
	}

    // getters & setters
    
}

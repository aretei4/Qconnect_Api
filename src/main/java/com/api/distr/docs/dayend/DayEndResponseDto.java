package com.api.distr.docs.dayend;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

public class DayEndResponseDto {

		private Long dayendId;
	    private Long deliveryId;
	    private String deliveryBoyName;

	    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
	    private LocalDate deliveryDate;

	    private String status;
	    private Double totalAmount;
	    private String rejectReason;

	    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
	    private LocalDateTime requestDate;

	    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
	    private LocalDateTime approvedAt;

	    
		public Long getDayendId() {
			return dayendId;
		}

		public void setDayendId(Long dayendId) {
			this.dayendId = dayendId;
		}

		public Long getDeliveryId() {
			return deliveryId;
		}

		public void setDeliveryId(Long deliveryId) {
			this.deliveryId = deliveryId;
		}

		public String getDeliveryBoyName() {
			return deliveryBoyName;
		}

		public void setDeliveryBoyName(String deliveryBoyName) {
			this.deliveryBoyName = deliveryBoyName;
		}

		public LocalDate getDeliveryDate() {
			return deliveryDate;
		}

		public void setDeliveryDate(LocalDate deliveryDate) {
			this.deliveryDate = deliveryDate;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
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

		public LocalDateTime getRequestDate() {
			return requestDate;
		}

		public void setRequestDate(LocalDateTime requestDate) {
			this.requestDate = requestDate;
		}

		public LocalDateTime getApprovedAt() {
			return approvedAt;
		}

		public void setApprovedAt(LocalDateTime approvedAt) {
			this.approvedAt = approvedAt;
		}
	    
	    
}

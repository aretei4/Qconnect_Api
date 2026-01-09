package com.api.distr.docs.sales.dto;

import java.sql.Date;

public class SalesEntryDto{

	 private String picklistNo;
	   
	    private String customerNo;
	    private String custDesc;
	    private String billingDate;
	   private String netValue;
	    private String updateDate;
	   // private Integer buId;
		public String getPicklistNo() {
			return picklistNo;
		}
		public void setPicklistNo(String picklistNo) {
			this.picklistNo = picklistNo;
		}
		public String getCustomerNo() {
			return customerNo;
		}
		public void setCustomerNo(String customerNo) {
			this.customerNo = customerNo;
		}
		public String getCustDesc() {
			return custDesc;
		}
		public void setCustDesc(String custDesc) {
			this.custDesc = custDesc;
		}
		public String getBillingDate() {
			return billingDate;
		}
		public void setBillingDate(String billingDate) {
			this.billingDate = billingDate;
		}
		public String getNetValue() {
			return netValue;
		}
		public void setNetValue(String netValue) {
			this.netValue = netValue;
		}
		public String getUpdateDate() {
			return updateDate;
		}
		public void setUpdateDate(String updateDate) {
			this.updateDate = updateDate;
		}
	

    // getters & setters
}


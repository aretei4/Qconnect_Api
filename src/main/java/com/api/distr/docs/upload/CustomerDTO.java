package com.api.distr.docs.upload;


import lombok.Data;

@Data
public class CustomerDTO {

  
    private String custNo;

 
    public String getCustNo() {
		return custNo;
	}
	public void setCustNo(String custNo) {
		this.custNo = custNo;
	}
	public String getCustDesc() {
		return custDesc;
	}
	public void setCustDesc(String custDesc) {
		this.custDesc = custDesc;
	}
	public String getCustMobile() {
		return custMobile;
	}
	public void setCustMobile(String custMobile) {
		this.custMobile = custMobile;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getPin() {
		return pin;
	}
	public void setPin(String pin) {
		this.pin = pin;
	}
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
	private String custDesc;


    private String custMobile;

    private String address;

    private String pin;

    private Double lat;
    private Double lon;
}



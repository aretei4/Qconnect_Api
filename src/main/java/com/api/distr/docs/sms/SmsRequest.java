package com.api.distr.docs.sms;

import lombok.Data;

@Data
class SmsRequest {
    private String mobile;
    public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	private String message;
}

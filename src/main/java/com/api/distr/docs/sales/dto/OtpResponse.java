package com.api.distr.docs.sales.dto;

public class OtpResponse {
    private String otp;
    private boolean success;

    public OtpResponse(String otp, boolean success) {
        this.otp = otp;
        this.success = success;
    }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
}


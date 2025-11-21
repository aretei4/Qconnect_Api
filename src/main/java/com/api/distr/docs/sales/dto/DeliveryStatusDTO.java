package com.api.distr.docs.sales.dto;

public class DeliveryStatusDTO {
    public String delivery_id;
    public String picklist_no;
    public boolean delivered;
    public boolean otp;
    public double payment_amount;
    public String payment_mode;
    public String reason;
    public String delivery_date;   // dd/MM/yyyy formatted
}


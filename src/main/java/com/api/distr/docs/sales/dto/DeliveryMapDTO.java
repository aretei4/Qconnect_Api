package com.api.distr.docs.sales.dto;

public class DeliveryMapDTO {
    public String picklist_no;
    public String deliveryBoyName;
    public String status;          // DELIVERED | FAILED | PENDING
    public Double lat;
    public Double lon;
    public String delivery_date;   // dd/MM/yyyy
    public String address;         // display address
    public int    sequence;        // stop order within agent's route
}

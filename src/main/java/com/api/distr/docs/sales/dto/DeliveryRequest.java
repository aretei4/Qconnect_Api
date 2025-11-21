package com.api.distr.docs.sales.dto;


import java.util.List;

public class DeliveryRequest {
    private String deliveryBoyId;
    private List<String> picklistNos;
    private CarInfo car;

    public String getDeliveryBoyId() { return deliveryBoyId; }
    public void setDeliveryBoyId(String deliveryBoyId) { this.deliveryBoyId = deliveryBoyId; }

    public List<String> getPicklistNos() { return picklistNos; }
    public void setPicklistNos(List<String> picklistNos) { this.picklistNos = picklistNos; }

    public CarInfo getCar() { return car; }
    public void setCar(CarInfo car) { this.car = car; }
}


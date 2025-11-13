package com.api.distr.docs.sales.dto;


import java.util.List;

public class DeliveryRequest {
    private int deliveryBoyId;
    private List<String> picklistNos;
    private CarInfo car;

    public int getDeliveryBoyId() { return deliveryBoyId; }
    public void setDeliveryBoyId(int deliveryBoyId) { this.deliveryBoyId = deliveryBoyId; }

    public List<String> getPicklistNos() { return picklistNos; }
    public void setPicklistNos(List<String> picklistNos) { this.picklistNos = picklistNos; }

    public CarInfo getCar() { return car; }
    public void setCar(CarInfo car) { this.car = car; }
}


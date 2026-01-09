package com.api.distr.docs.sales.dto;

public class DeliveryLoginResponse {
    private Long deliveryId;
    private String deliveryName;
    private Integer buId;

    public DeliveryLoginResponse(Long deliveryId, String deliveryName, Integer buId) {
        this.deliveryId = deliveryId;
        this.deliveryName = deliveryName;
        this.buId = buId;
    }

    public Long getDeliveryId() {
        return deliveryId;
    }

    public String getDeliveryName() {
        return deliveryName;
    }

    public Integer getBuId() {
        return buId;
    }
}


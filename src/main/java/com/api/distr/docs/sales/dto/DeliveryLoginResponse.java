package com.api.distr.docs.sales.dto;

public class DeliveryLoginResponse {
    private Long    deliveryId;
    private String  deliveryName;
    private Integer buId;
    private String  role;
    private String  token;   // JWT — present for users-table logins, null for delivery agents

    public DeliveryLoginResponse(Long deliveryId, String deliveryName, Integer buId, String role, String token) {
        this.deliveryId   = deliveryId;
        this.deliveryName = deliveryName;
        this.buId         = buId;
        this.role         = role;
        this.token        = token;
    }

    public Long    getDeliveryId()   { return deliveryId; }
    public String  getDeliveryName() { return deliveryName; }
    public Integer getBuId()         { return buId; }
    public String  getRole()         { return role; }
    public String  getToken()        { return token; }
}

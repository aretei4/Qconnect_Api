package com.api.distr.docs.dan.dto;

import java.util.List;

/**
 * Request body sent by the Android mobile app to POST /api/dan/returns.
 */
public class MobileReturnRequest {

    private String deliveryId;
    private Long   direId;       // stage_sales_entery.dire_id — primary transaction key
    private String ndType;       // "full" | "partial"
    private String reason;
    private List<MobileReturnItemDto> items;

    public String getDeliveryId()                       { return deliveryId; }
    public void   setDeliveryId(String v)               { this.deliveryId = v; }

    public Long   getDireId()                           { return direId; }
    public void   setDireId(Long v)                     { this.direId = v; }

    public String getNdType()                           { return ndType; }
    public void   setNdType(String v)                   { this.ndType = v; }

    public String getReason()                           { return reason; }
    public void   setReason(String v)                   { this.reason = v; }

    public List<MobileReturnItemDto> getItems()         { return items; }
    public void setItems(List<MobileReturnItemDto> v)   { this.items = v; }
}

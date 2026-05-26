package com.api.distr.docs.dayend;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DayEndDto {

    private String date;           // dd-MM-yyyy

    @JsonAlias("delivery_id")
    private Long deliveryId;

    @JsonAlias("total_amount")
    private Double totalAmount;

    /** Accepts both camelCase (picklistNos) and snake_case (picklist_nos). */
    @JsonProperty("picklistNos")
    @JsonAlias("picklist_nos")
    private List<String> picklistNos;

    @JsonAlias("reject_reason")
    private String rejectReason;

    @JsonAlias("dayend_id")
    private Long dayendId;

    public Long getDayendId()              { return dayendId; }
    public void setDayendId(Long v)        { this.dayendId = v; }

    public List<String> getPicklistNos()   { return picklistNos; }
    public void setPicklistNos(List<String> v) { this.picklistNos = v; }

    public String getDate()                { return date; }
    public void setDate(String v)          { this.date = v; }

    public Long getDeliveryId()            { return deliveryId; }
    public void setDeliveryId(Long v)      { this.deliveryId = v; }

    public Double getTotalAmount()         { return totalAmount; }
    public void setTotalAmount(Double v)   { this.totalAmount = v; }

    public String getRejectReason()        { return rejectReason; }
    public void setRejectReason(String v)  { this.rejectReason = v; }
}

package com.api.distr.docs.dayend;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class OnlineAgentDto {

    private Long   deliveryId;
    private String deliveryBoyName;
    private String status;           // STARTED | PENDING

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime startTime;

    private int    totalDeliveries;
    private int    deliveredCount;
    private double totalAmount;

    // ── getters / setters ─────────────────────────────────────────────────────

    public Long getDeliveryId()              { return deliveryId; }
    public void setDeliveryId(Long v)        { this.deliveryId = v; }

    public String getDeliveryBoyName()       { return deliveryBoyName; }
    public void setDeliveryBoyName(String v) { this.deliveryBoyName = v; }

    public String getStatus()                { return status; }
    public void setStatus(String v)          { this.status = v; }

    public LocalDateTime getStartTime()      { return startTime; }
    public void setStartTime(LocalDateTime v){ this.startTime = v; }

    public int getTotalDeliveries()          { return totalDeliveries; }
    public void setTotalDeliveries(int v)    { this.totalDeliveries = v; }

    public int getDeliveredCount()           { return deliveredCount; }
    public void setDeliveredCount(int v)     { this.deliveredCount = v; }

    public double getTotalAmount()           { return totalAmount; }
    public void setTotalAmount(double v)     { this.totalAmount = v; }
}

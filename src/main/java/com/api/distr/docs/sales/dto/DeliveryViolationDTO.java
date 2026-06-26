package com.api.distr.docs.sales.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeliveryViolationDTO {

    @JsonProperty("direId")       public Long   direId;
    @JsonProperty("agentId")      public Long   agentId;
    @JsonProperty("agentName")    public String agentName;
    @JsonProperty("custName")     public String custName;
    @JsonProperty("custNo")       public String custNo;
    @JsonProperty("invoiceNo")    public String invoiceNo;
    @JsonProperty("date")         public String date;
    @JsonProperty("delLat")       public double delLat;
    @JsonProperty("delLng")       public double delLng;
    @JsonProperty("custLat")      public double custLat;
    @JsonProperty("custLng")      public double custLng;
    @JsonProperty("netValue")     public double netValue;
    @JsonProperty("delivered")    public boolean delivered;
}

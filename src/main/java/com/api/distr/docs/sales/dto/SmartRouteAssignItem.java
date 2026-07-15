package com.api.distr.docs.sales.dto;

/**
 * One stop in a Smart Route assignment.
 * Sent as a JSON array from the SmartRoute frontend page.
 *
 * Example payload element:
 * {
 *   "picklist_no":     "E587P22657",
 *   "sequence":        1,
 *   "deliveryBoyId":   5,
 *   "deliveryBoyName": "Ravi Kumar",
 *   "address":         "Rajmahal Square, Bhubaneswar",
 *   "lat":             20.2961,
 *   "lon":             85.8245
 * }
 */
public class SmartRouteAssignItem {

    public String picklist_no;
    public Long   direId;        // optional — when present, used directly instead of picklist lookup
    public int    sequence;
    public Long   deliveryBoyId;
    public String deliveryBoyName;
    public String address;
    public Double lat;
    public Double lon;

    // Getters / setters (for Jackson)
    public String getPicklist_no()     { return picklist_no; }
    public void   setPicklist_no(String v) { picklist_no = v; }

    public Long getDireId()            { return direId; }
    public void setDireId(Long v)      { direId = v; }

    public int  getSequence()          { return sequence; }
    public void setSequence(int v)     { sequence = v; }

    public Long  getDeliveryBoyId()          { return deliveryBoyId; }
    public void  setDeliveryBoyId(Long v)    { deliveryBoyId = v; }

    public String getDeliveryBoyName()       { return deliveryBoyName; }
    public void   setDeliveryBoyName(String v) { deliveryBoyName = v; }

    public String getAddress()         { return address; }
    public void   setAddress(String v) { address = v; }

    public Double getLat()             { return lat; }
    public void   setLat(Double v)     { lat = v; }

    public Double getLon()             { return lon; }
    public void   setLon(Double v)     { lon = v; }
}

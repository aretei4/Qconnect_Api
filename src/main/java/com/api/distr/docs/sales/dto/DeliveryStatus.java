package com.api.distr.docs.sales.dto;

import java.util.List;

/**
 * Payload for POST /api/delivery/delivery-status
 *
 * Multiple payment modes — each mode carries its own amount:
 *
 *   "paymentModes": [
 *     { "mode": "CASH", "amount": 1000.00 },
 *     { "mode": "UPI",  "amount": 500.00  }
 *   ]
 *
 * Total payment = sum of all mode amounts (computed automatically).
 * Stored in DB as:  payment_mode  = "CASH:1000.0,UPI:500.0"
 *                   payment_amount = 1500.0
 */
public class DeliveryStatus {

    private Long    delivery_id;
    private boolean delivered;
    private boolean otp;
    private String  picklistNo;
    private String  reason;
    private Double  lat;
    private Double  lon;

    /** One entry per payment mode, each with its own amount. */
    private List<PaymentModeEntry> paymentModes;

    // ── computed helpers ──────────────────────────────────────────────────────

    /**
     * Returns total payment amount = sum of all mode amounts.
     * Used when saving to delivery_status.payment_amount column.
     */
    public double getTotalPaymentAmount() {
        if (paymentModes == null || paymentModes.isEmpty()) return 0.0;
        return paymentModes.stream()
                .mapToDouble(PaymentModeEntry::getAmount)
                .sum();
    }

    /**
     * Returns payment detail as a comma-separated string for DB storage.
     * e.g. "CASH:1000.0,UPI:500.0"
     */
    public String getPaymentModeDbValue() {
        if (paymentModes == null || paymentModes.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (PaymentModeEntry e : paymentModes) {
            if (sb.length() > 0) sb.append(",");
            sb.append(e.getMode()).append(":").append(e.getAmount());
        }
        return sb.toString();
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public List<PaymentModeEntry> getPaymentModes() { return paymentModes; }
    public void setPaymentModes(List<PaymentModeEntry> paymentModes) { this.paymentModes = paymentModes; }

    public Long getDelivery_id() { return delivery_id; }
    public void setDelivery_id(Long delivery_id) { this.delivery_id = delivery_id; }

    public boolean isDelivered() { return delivered; }
    public void setDelivered(boolean delivered) { this.delivered = delivered; }

    public boolean isOtp() { return otp; }
    public void setOtp(boolean otp) { this.otp = otp; }

    public String getPicklistNo() { return picklistNo; }
    public void setPicklistNo(String picklistNo) { this.picklistNo = picklistNo; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLon() { return lon; }
    public void setLon(Double lon) { this.lon = lon; }
}

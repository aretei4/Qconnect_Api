package com.api.distr.docs.sales.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Payload for POST /api/delivery/delivery-status
 *
 * Multiple payment modes — each mode carries its own amount plus optional details:
 *
 *   "paymentModes": [
 *     { "mode": "CHEQUE", "amount": 3900, "chequeNo": "56789", "bankName": "uti" },
 *     { "mode": "UPI",    "amount": 60,   "referenceNo": "ruiio" }
 *   ]
 *
 * Total payment = sum of all mode amounts (computed automatically).
 * Stored in DB as:  payment_mode  = JSON array string (all details preserved)
 *                   payment_amount = sum of amounts
 */
public class DeliveryStatus {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long    delivery_id;
    private boolean delivered;
    private boolean otp;
    private String  picklistNo;
    private String  reason;
    private Double  lat;
    private Double  lon;

    /** One entry per payment mode, each with its own amount and optional details. */
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
     * Serializes full payment mode list as a JSON string for DB storage.
     * Preserves chequeNo, bankName, referenceNo alongside mode and amount.
     *
     * e.g. [{"mode":"CHEQUE","amount":3900.0,"chequeNo":"56789","bankName":"uti"},
     *        {"mode":"UPI","amount":60.0,"referenceNo":"ruiio"}]
     *
     * Falls back to "MODE:AMOUNT,..." format if JSON serialization fails.
     */
    public String getPaymentModeDbValue() {
        if (paymentModes == null || paymentModes.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(paymentModes);
        } catch (JsonProcessingException e) {
            // Fallback: simple format
            StringBuilder sb = new StringBuilder();
            for (PaymentModeEntry entry : paymentModes) {
                if (sb.length() > 0) sb.append(",");
                sb.append(entry.getMode()).append(":").append(entry.getAmount());
            }
            return sb.toString();
        }
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

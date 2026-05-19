package com.api.distr.docs.sales.dto;

import java.util.ArrayList;
import java.util.List;

public class DeliveryStatusDTO {

    public String  delivery_id;
    public String  deliveryBoyName;      // from delivery_master.delivery_name
    public String  picklist_no;
    public String  status;               // DELIVERED | FAILED | PENDING
    public boolean otp;
    public double  payment_amount;       // total (sum of all modes)
    public List<PaymentModeEntry> paymentModes;  // parsed from "CASH:1000.0,UPI:500.0"
    public String  reason;
    public String  delivery_date;        // dd/MM/yyyy

    /**
     * Parses the DB string "CASH:1000.0,UPI:500.0" into a List<PaymentModeEntry>.
     * Handles legacy single-mode strings like "CASH" (amount defaults to payment_amount).
     */
    public static List<PaymentModeEntry> parsePaymentModes(String raw, double totalAmount) {
        List<PaymentModeEntry> list = new ArrayList<>();
        if (raw == null || raw.isBlank()) return list;

        String[] parts = raw.split(",");
        for (String part : parts) {
            part = part.trim();
            PaymentModeEntry entry = new PaymentModeEntry();
            if (part.contains(":")) {
                String[] kv = part.split(":", 2);
                entry.setMode(kv[0].trim());
                try {
                    entry.setAmount(Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException e) {
                    entry.setAmount(0.0);
                }
            } else {
                // Legacy: plain mode name with no amount → use total
                entry.setMode(part);
                entry.setAmount(totalAmount);
            }
            list.add(entry);
        }
        return list;
    }
}

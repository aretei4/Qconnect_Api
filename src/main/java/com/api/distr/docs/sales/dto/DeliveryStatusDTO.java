package com.api.distr.docs.sales.dto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DeliveryStatusDTO {

    public String  delivery_id;
    public String  deliveryBoyName;      // from delivery_master.delivery_name
    public Long    direId;               // stage_sales_entery.dire_id — unique transaction reference
    public String  invoiceNo;            // stage_sales_entery.sales_order_no
    public String  custDesc;             // stage_sales_entery.cust_desc — customer name
    public double  netValue;             // stage_sales_entery.net_value — invoice value
    public String  picklist_no;
    public String  status;               // DELIVERED | FAILED | PENDING
    public boolean otp;
    public double  payment_amount;       // total (sum of all modes)
    public List<PaymentModeEntry> paymentModes;  // parsed from JSON array or CSV
    public String  reason;
    public String  delivery_date;        // dd/MM/yyyy

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parses the DB payment_mode column into a List<PaymentModeEntry>.
     *
     * Supports two formats:
     *   JSON array: [{"mode":"CHEQUE","amount":3900,"chequeNo":"56789","bankName":"uti"}]
     *   Legacy CSV: "CASH:1000.0,UPI:500.0"  or  "CASH" (amount = totalAmount)
     */
    public static List<PaymentModeEntry> parsePaymentModes(String raw, double totalAmount) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();

        String trimmed = raw.trim();

        // ── JSON array format ─────────────────────────────────────────────────
        if (trimmed.startsWith("[")) {
            try {
                PaymentModeEntry[] arr = MAPPER.readValue(trimmed, PaymentModeEntry[].class);
                return new ArrayList<>(Arrays.asList(arr));
            } catch (Exception ignored) {
                // fall through to CSV parser
            }
        }

        // ── Legacy CSV format: "CASH:1000.0,UPI:500.0" ───────────────────────
        List<PaymentModeEntry> list = new ArrayList<>();
        String[] parts = trimmed.split(",");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
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
                // Plain mode name only — use total as amount
                entry.setMode(part);
                entry.setAmount(totalAmount);
            }
            list.add(entry);
        }
        return list;
    }
}

package com.api.distr.docs.sales.dto;

/**
 * A single payment mode with its collected amount.
 * Used inside DeliveryStatus.paymentModes list.
 *
 * Example JSON:
 *   { "mode": "CASH", "amount": 1000.00 }
 */
public class PaymentModeEntry {

    /** Accepted values: CASH | UPI | CHEQUE | CREDIT | BANK_TRANSFER | ONLINE */
    private String mode;

    /** Amount collected via this mode */
    private double amount;

    public PaymentModeEntry() {}
    public PaymentModeEntry(String mode, double amount) {
        this.mode   = mode;
        this.amount = amount;
    }

    public String getMode()   { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
}

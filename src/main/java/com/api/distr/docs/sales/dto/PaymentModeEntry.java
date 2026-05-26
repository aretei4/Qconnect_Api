package com.api.distr.docs.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A single payment mode with its collected amount and optional details.
 * Used inside DeliveryStatus.paymentModes list.
 *
 * Example JSON:
 *   { "mode": "CHEQUE", "amount": 3900, "chequeNo": "56789", "bankName": "uti" }
 *   { "mode": "UPI",    "amount": 60,   "referenceNo": "ruiio" }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentModeEntry {

    /** Accepted values: CASH | UPI | CHEQUE | CREDIT | BANK_TRANSFER | ONLINE */
    private String mode;

    /** Amount collected via this mode */
    private double amount;

    /** Cheque number — populated when mode = CHEQUE */
    private String chequeNo;

    /** Bank name — populated when mode = CHEQUE */
    private String bankName;

    /** UTR / reference number — populated when mode = UPI or BANK_TRANSFER */
    private String referenceNo;

    public PaymentModeEntry() {}

    public PaymentModeEntry(String mode, double amount) {
        this.mode   = mode;
        this.amount = amount;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getMode()   { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getChequeNo() { return chequeNo; }
    public void setChequeNo(String chequeNo) { this.chequeNo = chequeNo; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getReferenceNo() { return referenceNo; }
    public void setReferenceNo(String referenceNo) { this.referenceNo = referenceNo; }
}

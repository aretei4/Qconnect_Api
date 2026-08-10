package com.api.distr.docs.mobile.dto;

import java.util.ArrayList;
import java.util.List;

/** Step 2 — one store/invoice card with its pre-loaded return rows. */
public class MobileInvoiceDto {

    /** A single settle-returns line. Amounts are null until the agent fills them in. */
    public static class ReturnRow {
        private String  rowId;      // e.g. "1_1" — card index + row index
        private int     sl;
        private Double  billQty;
        private Double  billAmt;
        private Double  retQty;
        private Double  retAmt;

        public ReturnRow() {}

        public ReturnRow(String rowId, int sl, Double billQty, Double billAmt, Double retQty, Double retAmt) {
            this.rowId   = rowId;
            this.sl      = sl;
            this.billQty = billQty;
            this.billAmt = billAmt;
            this.retQty  = retQty;
            this.retAmt  = retAmt;
        }

        public String getRowId()            { return rowId; }
        public void   setRowId(String v)    { this.rowId = v; }
        public int    getSl()               { return sl; }
        public void   setSl(int v)          { this.sl = v; }
        public Double getBillQty()          { return billQty; }
        public void   setBillQty(Double v)  { this.billQty = v; }
        public Double getBillAmt()          { return billAmt; }
        public void   setBillAmt(Double v)  { this.billAmt = v; }
        public Double getRetQty()           { return retQty; }
        public void   setRetQty(Double v)   { this.retQty = v; }
        public Double getRetAmt()           { return retAmt; }
        public void   setRetAmt(Double v)   { this.retAmt = v; }
    }

    private int    no;          // card number shown as #1, #2 …
    private String storeName;
    private String invoiceNo;
    private List<ReturnRow> rows = new ArrayList<>();

    public MobileInvoiceDto() {}

    public MobileInvoiceDto(int no, String storeName, String invoiceNo, List<ReturnRow> rows) {
        this.no        = no;
        this.storeName = storeName;
        this.invoiceNo = invoiceNo;
        this.rows      = rows;
    }

    public int    getNo()                    { return no; }
    public void   setNo(int v)               { this.no = v; }

    public String getStoreName()             { return storeName; }
    public void   setStoreName(String v)     { this.storeName = v; }

    public String getInvoiceNo()             { return invoiceNo; }
    public void   setInvoiceNo(String v)     { this.invoiceNo = v; }

    public List<ReturnRow> getRows()         { return rows; }
    public void   setRows(List<ReturnRow> v) { this.rows = v; }
}

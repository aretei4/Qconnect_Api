package com.api.distr.docs.mobile;

import com.api.distr.docs.dan.DanApprovalService;
import com.api.distr.docs.dan.DanService;
import com.api.distr.docs.dan.dto.DanApprovalRequest;
import com.api.distr.docs.dan.dto.DanApprovalStatusDto;
import com.api.distr.docs.dan.dto.DanListDto;
import com.api.distr.docs.dan.dto.DanPaymentDto;
import com.api.distr.docs.dan.dto.DanPicklistDto;
import com.api.distr.docs.dan.dto.DanReturnDto;
import com.api.distr.docs.dan.dto.WebReturnItemDto;
import com.api.distr.docs.mobile.dto.MobileAgentDto;
import com.api.distr.docs.mobile.dto.MobileApproveRequest;
import com.api.distr.docs.mobile.dto.MobileInvoiceDto;
import com.api.distr.docs.mobile.dto.MobilePaymentSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mobile DAN-close flow — backed by the SAME data as the web DAN Close page
 * (com.api.distr.docs.dan). This class only reshapes DanService results into
 * the mobile wizard's DTOs; there is no separate storage.
 *
 * rowId convention: "{direId}_{index}" so the approve step can group the
 * returns the phone sends back by their originating invoice.
 */
@Service
public class MobileDanService {

    private static final Logger log = LoggerFactory.getLogger(MobileDanService.class);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final DanService danService;
    private final DanApprovalService approvalService;

    public MobileDanService(DanService danService, DanApprovalService approvalService) {
        this.danService      = danService;
        this.approvalService = approvalService;
    }

    // ── Step 1 — open DANs ────────────────────────────────────────────────────

    public List<MobileAgentDto> getOpenDans() {
        List<MobileAgentDto> out = new ArrayList<>();
        for (DanListDto d : danService.getMobileDans()) {
            out.add(new MobileAgentDto(
                    d.getAgentId(),
                    d.getAgentName(),
                    d.getDanId(),
                    d.getDanCode(),
                    d.getDate() != null ? d.getDate().format(DMY) : ""));
        }
        log.info("getOpenDans: {} open DAN(s)", out.size());
        return out;
    }

    // ── Step 2 — invoices with return rows ────────────────────────────────────

    public List<MobileInvoiceDto> getInvoices(Long danId) {
        List<MobileInvoiceDto> out = new ArrayList<>();

        List<DanPicklistDto> picklists = picklistsOf(danId);
        List<Long> allDireIds = picklists.stream()
                .map(DanPicklistDto::getDireId)
                .filter(java.util.Objects::nonNull)
                .toList();
        // Undelivered invoices are a full return — same rule the desktop
        // DAN Close page applies when seeding its return grid.
        Map<Long, Boolean> delivered = danService.getDeliveredFlags(allDireIds);

        for (DanPicklistDto p : picklists) {
            Long direId = p.getDireId();
            List<MobileInvoiceDto.ReturnRow> rows = new ArrayList<>();

            if (direId != null) {
                List<DanReturnDto> saved = danService.getReturnsByDireId(direId);
                int sl = 1;
                for (DanReturnDto r : saved) {
                    rows.add(new MobileInvoiceDto.ReturnRow(
                            direId + "_" + sl, sl,
                            (double) r.getBillQty(), r.getBillAmt(),
                            (double) r.getReturnQty(), r.getReturnAmt()));
                    sl++;
                }
                if (rows.isEmpty()) {
                    if (Boolean.FALSE.equals(delivered.get(direId))) {
                        // Not delivered and nothing settled yet → whole invoice comes back
                        double net = p.getNetValue();
                        rows.add(new MobileInvoiceDto.ReturnRow(
                                direId + "_1", 1, 1.0, net, 1.0, net));
                    } else {
                        // Delivered (or unknown) — one blank row for the agent to fill in
                        rows.add(new MobileInvoiceDto.ReturnRow(direId + "_1", 1, null, null, null, null));
                    }
                }
            }

            out.add(new MobileInvoiceDto(
                    p.getNo(),
                    p.getCustName() != null ? p.getCustName() : "",
                    p.getInvoiceNo() != null && !p.getInvoiceNo().isBlank()
                            ? p.getInvoiceNo() : p.getPicklistNo(),
                    rows));
        }

        log.info("getInvoices: danId={}, {} invoice card(s)", danId, out.size());
        return out;
    }

    // ── Step 3 — payment summary ──────────────────────────────────────────────

    public MobilePaymentSummaryDto getPaymentSummary(Long danId) {
        List<DanPicklistDto> picklists = picklistsOf(danId);
        List<Long> direIds = picklists.stream()
                .map(DanPicklistDto::getDireId)
                .filter(java.util.Objects::nonNull)
                .toList();

        double netValue  = picklists.stream().mapToDouble(DanPicklistDto::getNetValue).sum();
        double returnAmt = danService.getReturnTotal(direIds);
        Map<String, Double> paid = danService.getPaymentTotals(direIds);

        log.info("getPaymentSummary: danId={}, net={}, returns={}", danId, netValue, returnAmt);
        return new MobilePaymentSummaryDto(
                paid.getOrDefault("cash",   0.0),
                paid.getOrDefault("upi",    0.0),
                paid.getOrDefault("cheque", 0.0),
                paid.getOrDefault("neft",   0.0),
                netValue,
                returnAmt);
    }

    // ── Step 2 — storekeeper approval ─────────────────────────────────────────

    /**
     * Saves the settled returns, then records the STOREKEEPER sign-off.
     * Called by the phone's "Approve & Next" button on step 2.
     */
    @Transactional
    public DanApprovalStatusDto approveStorekeeper(Long danId, MobileApproveRequest req) {
        // Group the phone's rows by their originating dire_id and persist them
        Map<Long, List<WebReturnItemDto>> byDire = new LinkedHashMap<>();
        if (req.getReturns() != null) {
            for (Map.Entry<String, Map<String, Double>> e : req.getReturns().entrySet()) {
                Long direId = direIdOf(e.getKey());
                if (direId == null) continue;
                Map<String, Double> v = e.getValue();

                WebReturnItemDto item = new WebReturnItemDto();
                item.setSerial("");
                item.setDescription("");
                item.setBillQty(  num(v.get("billQty")));
                item.setBillAmt(  num(v.get("billAmt")));
                item.setReturnQty(num(v.get("retQty")));
                item.setReturnAmt(num(v.get("retAmt")));
                item.setReason("");
                item.setCustom(true);

                byDire.computeIfAbsent(direId, k -> new ArrayList<>()).add(item);
            }
        }
        byDire.forEach(danService::saveReturnsByDireId);

        DanApprovalRequest approval = new DanApprovalRequest();
        approval.setStage(DanApprovalService.STAGE_STOREKEEPER);
        approval.setAction("APPROVED");
        approval.setApprovedBy(req.getApprovedBy());
        approval.setRemarks(req.getRemarks());

        log.info("approveStorekeeper: danId={}, returnRows={}", danId, byDire.size());
        return approvalService.record(danId, approval);
    }

    // ── Step 3 — per-invoice payment detail ───────────────────────────────────

    /**
     * Per-invoice payment breakdown for the accounts desk — invoice no, customer
     * name, net value, and each payment mode with its cheque/reference detail.
     * Reuses the web DAN-close report data so both screens stay consistent.
     */
    public List<com.api.distr.docs.dan.dto.DanReportDetailDto.InvoiceRow> getPaymentDetail(Long danId) {
        var detail = danService.getReportDetail(danId);
        log.info("getPaymentDetail: danId={}, {} invoice(s)", danId, detail.getInvoices().size());
        return detail.getInvoices();
    }

    // ── Step 3 — accounts approval ────────────────────────────────────────────

    /**
     * Persists the collected payments and records the ACCOUNTS sign-off.
     * Called by the phone's "Approve" button on step 3.
     *
     * The DAN is NOT closed here — with both desks signed off it becomes
     * APPROVED, and closing it remains a separate explicit action.
     *
     * The phone collects ONE payment figure per mode for the whole DAN, while
     * payment_details is keyed per invoice — so the collected amount is split
     * across invoices in proportion to their net value, with any rounding
     * remainder added to the last invoice so the totals reconcile exactly.
     */
    @Transactional
    public DanApprovalStatusDto approveAccounts(Long danId, MobileApproveRequest req) {
        List<DanPicklistDto> picklists = picklistsOf(danId);

        double collected = req.getPayments() == null ? 0.0
                : req.getPayments().values().stream()
                     .filter(java.util.Objects::nonNull)
                     .mapToDouble(Double::doubleValue).sum();

        double netTotal = picklists.stream().mapToDouble(DanPicklistDto::getNetValue).sum();
        List<DanPicklistDto> withDire = picklists.stream()
                .filter(p -> p.getDireId() != null).toList();

        double allocated = 0.0;
        for (int i = 0; i < withDire.size(); i++) {
            DanPicklistDto p = withDire.get(i);
            boolean last = (i == withDire.size() - 1);

            double share = last
                    ? collected - allocated                             // absorb rounding
                    : (netTotal > 0 ? collected * p.getNetValue() / netTotal : 0.0);
            share = Math.max(0, Math.round(share * 100.0) / 100.0);
            allocated += share;

            DanPaymentDto pay = new DanPaymentDto();
            pay.setDelivered(true);
            pay.setPaymentAmount(share);
            pay.setPaymentMode(modeJson(req.getPayments(), share, collected));
            danService.savePayment(p.getDireId(), pay);
        }

        // NOTE: the DAN is deliberately NOT closed here. Recording the accounts
        // sign-off moves it to APPROVED once both desks have signed; closing it
        // (delivery_assignments → status 10, failed rows deleted) stays a
        // separate, explicit step via POST /api/dan/{danId}/submit.
        DanApprovalRequest approval = new DanApprovalRequest();
        approval.setStage(DanApprovalService.STAGE_ACCOUNTS);
        approval.setAction("APPROVED");
        approval.setApprovedBy(req.getApprovedBy());
        approval.setRemarks(req.getRemarks());

        log.info("approveAccounts: danId={}, collected={}, invoices={}", danId, collected, withDire.size());
        return approvalService.record(danId, approval);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<DanPicklistDto> picklistsOf(Long danId) {
        return danService.getMobileDans().stream()
                .filter(d -> d.getDanId() != null && d.getDanId().equals(danId))
                .findFirst()
                .map(d -> d.getPicklists() != null ? d.getPicklists() : List.<DanPicklistDto>of())
                .orElseGet(List::of);
    }

    /** "1234_2" → 1234 */
    private Long direIdOf(String rowId) {
        try {
            return Long.parseLong(rowId.split("_")[0]);
        } catch (Exception e) {
            log.warn("approve: skipping unrecognised rowId '{}'", rowId);
            return null;
        }
    }

    private double num(Double d) { return d != null ? d : 0.0; }

    /** Builds this invoice's payment_mode JSON, keeping each mode's proportional share. */
    private String modeJson(Map<String, Double> payments, double share, double collected) {
        List<com.api.distr.docs.sales.dto.PaymentModeEntry> modes = new ArrayList<>();
        if (payments != null && collected > 0) {
            for (Map.Entry<String, Double> e : payments.entrySet()) {
                double amt = num(e.getValue());
                if (amt <= 0) continue;
                double part = Math.round(share * amt / collected * 100.0) / 100.0;
                if (part > 0) {
                    modes.add(new com.api.distr.docs.sales.dto.PaymentModeEntry(
                            e.getKey().toUpperCase(), part));
                }
            }
        }
        return com.api.distr.docs.sales.dto.PaymentDetailsUtil.toJson(modes);
    }
}

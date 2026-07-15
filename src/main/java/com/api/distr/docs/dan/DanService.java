package com.api.distr.docs.dan;

import com.api.distr.docs.dan.dto.DanListDto;
import com.api.distr.docs.dan.dto.DanPaymentDto;
import com.api.distr.docs.dan.dto.DanReturnDto;
import com.api.distr.docs.dan.dto.MobileReturnItemDto;
import com.api.distr.docs.dan.dto.MobileReturnRequest;
import com.api.distr.docs.dan.dto.PamtReturnItem;
import com.api.distr.docs.dan.dto.WebReturnItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DanService {

    private static final Logger log = LoggerFactory.getLogger(DanService.class);
    private final DanRepository repo;

    public DanService(DanRepository repo) { this.repo = repo; }

    // ── DAN list ──────────────────────────────────────────────────────────────

    public List<DanListDto> getActiveDans() {
        return repo.getActiveDans(LocalDate.now());
    }

    // ── DAN Close Report ──────────────────────────────────────────────────────

    public List<com.api.distr.docs.dan.dto.DanReportRowDto> getReport(LocalDate fromDate, LocalDate toDate, Long agentId) {
        return repo.getReport(fromDate, toDate, agentId);
    }

    public com.api.distr.docs.dan.dto.DanReportDetailDto getReportDetail(long danId) {
        return repo.getReportDetail(danId);
    }

    // ── Returns ───────────────────────────────────────────────────────────────

    public List<DanReturnDto> getReturnsByDirId(long dirId) {
        return repo.getReturnsByDirId(dirId);
    }

    public List<DanReturnDto> getReturnsByDelivery(long deliveryId) {
        return repo.getReturnsByDelivery(deliveryId);
    }

    public List<DanReturnDto> getReturnsByDireId(long direId) {
        return repo.getReturnsByDireId(direId);
    }

    public long saveReturnsByDireId(long direId, List<WebReturnItemDto> items) {
        return repo.saveReturnsByDireId(direId, items);
    }

    /**
     * POST /api/dan/returns — Android mobile entry point.
     *
     * Maps MobileReturnRequest → DanReturnDto list, saves, and returns the
     * newly generated dir_id (unique per transaction across all submissions).
     */
    public long saveMobileReturn(MobileReturnRequest request) {
        if (request.getDeliveryId() == null || request.getDeliveryId().isBlank())
            throw new IllegalArgumentException("deliveryId is required");
        if (request.getDireId() == null || request.getDireId() <= 0)
            throw new IllegalArgumentException("direId is required");

        long deliveryId;
        try {
            deliveryId = Long.parseLong(request.getDeliveryId().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("deliveryId must be numeric: " + request.getDeliveryId());
        }

        Long   direId = request.getDireId();
        String ndType = request.getNdType() != null ? request.getNdType() : "partial";

        // Map mobile items → DanReturnDto; skip rows with no return quantity
        List<DanReturnDto> rows = new ArrayList<>();
        if (request.getItems() != null) {
            for (MobileReturnItemDto item : request.getItems()) {
                if (item.getReturnQty() <= 0 && item.getReturnAmt() <= 0) continue;

                DanReturnDto dto = new DanReturnDto();
                dto.setSerial(item.getSerial()    != null ? item.getSerial()    : "");
                dto.setDescription(item.getDesc() != null ? item.getDesc()      : "");
                dto.setBillQty((int) item.getBillQty());
                dto.setBillAmt(item.getBillAmt());
                dto.setReturnQty((int) item.getReturnQty());
                dto.setReturnAmt(item.getReturnAmt());

                // Item-level reason takes priority; fall back to top-level reason
                String reason = (item.getReason() != null && !item.getReason().isBlank())
                        ? item.getReason()
                        : (request.getReason() != null ? request.getReason() : "");
                dto.setReason(reason);
                dto.setCustom(true);
                rows.add(dto);
            }
        }

        long dirId = repo.saveReturns(deliveryId, direId, ndType, rows);
        log.info("saveMobileReturn: dirId={}, deliveryId={}, direId={}, ndType={}, rows={}",
                dirId, deliveryId, direId, ndType, rows.size());
        return dirId;
    }

    // ── Pamt check ────────────────────────────────────────────────────────────

    public List<PamtReturnItem> getPamtReturns(long deliveryId) {
        return repo.getPamtReturns(deliveryId);
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    public void savePayment(long direId, DanPaymentDto dto) {
        repo.savePayment(direId, dto);
    }

    // ── Submit DAN ────────────────────────────────────────────────────────────

    public void submitDan(Long danId) {
        repo.submitDan(danId);
    }
}

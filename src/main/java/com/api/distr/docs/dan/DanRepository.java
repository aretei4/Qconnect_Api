package com.api.distr.docs.dan;

import com.api.distr.docs.dan.dto.DanListDto;
import com.api.distr.docs.dan.dto.DanPaymentDto;
import com.api.distr.docs.dan.dto.DanPicklistDto;
import com.api.distr.docs.dan.dto.DanReturnDto;
import com.api.distr.docs.dan.dto.PamtReturnItem;
import com.api.distr.docs.dan.dto.WebReturnItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * dan_returns actual columns (13):
 *   id, serial, description, bill_qty, bill_amt, return_qty, return_amt,
 *   reason, is_custom, created_at, dire_id, delivery_id, nd_type
 */
@Repository
public class DanRepository {

    private static final Logger log = LoggerFactory.getLogger(DanRepository.class);
    private final JdbcTemplate jdbc;
    private final com.api.distr.docs.sales.repo.DeliveryRepository deliveryRepository;
    private final DanApprovalRepository approvalRepository;

    public DanRepository(JdbcTemplate jdbc,
                         com.api.distr.docs.sales.repo.DeliveryRepository deliveryRepository,
                         DanApprovalRepository approvalRepository) {
        this.jdbc = jdbc;
        this.deliveryRepository = deliveryRepository;
        this.approvalRepository = approvalRepository;
    }

    // ── Returns: save ─────────────────────────────────────────────────────────

    /**
     * Deletes any previous rows for this delivery_id + dire_id, then inserts
     * all return rows. Returns the dire_id as the transaction reference.
     */
    public long saveReturns(long deliveryId, Long direId,
                            String ndType, List<DanReturnDto> rows) {

        // Remove previous submission for this agent + dire_id
        if (direId != null && direId > 0) {
            jdbc.update("DELETE FROM dan_returns WHERE delivery_id = ? AND dire_id = ?",
                    deliveryId, direId);
        } else {
            jdbc.update("DELETE FROM dan_returns WHERE delivery_id = ?", deliveryId);
        }

        if (!rows.isEmpty()) {
            String sql = "INSERT INTO dan_returns " +
                         "  (dire_id, delivery_id, nd_type, " +
                         "   serial, description, bill_qty, bill_amt, " +
                         "   return_qty, return_amt, reason, is_custom) " +
                         "VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            for (DanReturnDto r : rows) {
                jdbc.update(sql,
                        direId, deliveryId, ndType,
                        r.getSerial(), r.getDescription(),
                        r.getBillQty(), r.getBillAmt(),
                        r.getReturnQty(), r.getReturnAmt(),
                        r.getReason(), r.isCustom());
            }
        }

        log.info("saveReturns: direId={}, deliveryId={}, rows={}", direId, deliveryId, rows.size());
        return direId != null ? direId : 0L;
    }

    // ── Returns: fetch ────────────────────────────────────────────────────────

    /** Fetch return items by dire_id. */
    public List<DanReturnDto> getReturnsByDireId(long direId) {
        return jdbc.query(
                "SELECT * FROM dan_returns WHERE dire_id = ? ORDER BY id",
                new Object[]{direId}, DanRepository::mapReturn);
    }

    /** Kept for backward compatibility — queries by auto-generated id. */
    public List<DanReturnDto> getReturnsByDirId(long id) {
        return jdbc.query(
                "SELECT * FROM dan_returns WHERE id = ? ORDER BY id",
                new Object[]{id}, DanRepository::mapReturn);
    }

    /** Fetch all returns for a delivery agent. */
    public List<DanReturnDto> getReturnsByDelivery(long deliveryId) {
        return jdbc.query(
                "SELECT * FROM dan_returns WHERE delivery_id = ? ORDER BY id DESC",
                new Object[]{deliveryId}, DanRepository::mapReturn);
    }

    // ── DAN list ──────────────────────────────────────────────────────────────

    /** DANs whose status is NOT one of {@code excludeStatuses}. */
    public List<DanListDto> getActiveDans(LocalDate date, int... excludeStatuses) {
        return getDansWhere("da.status NOT IN (" + csv(excludeStatuses) + ")");
    }

    /** DANs whose status IS one of {@code includeStatuses}. */
    public List<DanListDto> getDansWithStatus(LocalDate date, int... includeStatuses) {
        return getDansWhere("da.status IN (" + csv(includeStatuses) + ")");
    }

    /** Renders int codes as a SQL list. Safe: ints only, never caller text. */
    private static String csv(int... codes) {
        return java.util.Arrays.stream(codes)
                .mapToObj(Integer::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private List<DanListDto> getDansWhere(String statusPredicate) {
        String sql = """
            SELECT da.id AS dan_id, da.delivery_id, da.delivery_date,
                   (SELECT STRING_AGG(ds.dire_id::text, ',' ORDER BY ds.dire_id)
                    FROM delivery_status ds
                    LEFT JOIN delivery_assignments das ON das.dire_id = ds.dire_id
                    WHERE ds.delivery_id = da.delivery_id
                      AND ds.delivery_date::date = da.delivery_date
                      AND COALESCE(das.status, 0) != 10) AS dire_ids,
                   dm.delivery_name AS agent_name
            FROM dayend_approval da
            LEFT JOIN delivery_master dm ON da.delivery_id = dm.delivery_id
            WHERE %s
            ORDER BY da.id
            """.formatted(statusPredicate);

        return jdbc.query(sql, new Object[]{}, (rs, rn) -> {
            long      danId   = rs.getLong("dan_id");
            long      delId   = rs.getLong("delivery_id");
            LocalDate danDate = rs.getDate("delivery_date").toLocalDate();
            DanListDto dto    = new DanListDto();
            dto.setDanId(danId);
            dto.setDanCode(buildDanCode(danDate, danId));
            dto.setDate(danDate);
            dto.setAgentId(String.valueOf(delId));
            dto.setAgentName(rs.getString("agent_name") != null ? rs.getString("agent_name") : "Agent " + delId);
            dto.setAgentCode("#" + delId);
            dto.setPicklists(loadPicklistsByDireIds(rs.getString("dire_ids")));
            return dto;
        });
    }

    // ── Pamt check ───────────────────────────────────────────────────────────

    /**
     * Returns all dan_returns rows with nd_type='pamt' for the given delivery agent today.
     * These are partial returns where only an amount was recorded — full details are missing.
     * Joins stage_sales_entery to include customer/invoice info for navigation.
     */
    public List<PamtReturnItem> getPamtReturns(long deliveryId) {
        String sql = """
            SELECT DISTINCT ON (dr.dire_id)
                   dr.dire_id,
                   dr.return_amt,
                   COALESCE(sse.cust_desc,   '')            AS cust_desc,
                   COALESCE(sse.sales_order_no, '')         AS invoice_no,
                   COALESCE(sse.picklist_no, '')            AS picklist_no,
                   COALESCE(sse.customer_no, '')            AS customer_no,
                   COALESCE(sse.net_value, 0.0)             AS net_value
              FROM dan_returns dr
              LEFT JOIN stage_sales_entery sse ON sse.dire_id = dr.dire_id
             WHERE dr.delivery_id = ?
               AND dr.nd_type = 'pamt'
               AND DATE(dr.created_at) = CURRENT_DATE
             ORDER BY dr.dire_id
            """;
        return jdbc.query(sql, new Object[]{deliveryId}, (rs, rn) -> new PamtReturnItem(
                rs.getLong("dire_id"),
                rs.getDouble("return_amt"),
                rs.getString("cust_desc"),
                rs.getString("invoice_no"),
                rs.getString("picklist_no"),
                rs.getString("customer_no"),
                rs.getDouble("net_value")
        ));
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    /**
     * delivered flag per dire_id. A dire with no delivery_status row is absent
     * from the map — the caller decides what that means.
     */
    public java.util.Map<Long, Boolean> getDeliveredFlags(List<Long> direIds) {
        java.util.Map<Long, Boolean> out = new java.util.HashMap<>();
        if (direIds == null || direIds.isEmpty()) return out;

        String sql = "SELECT dire_id, delivered FROM delivery_status WHERE dire_id IN ("
                + direIds.stream().map(x -> "?").collect(java.util.stream.Collectors.joining(",")) + ")";
        jdbc.query(sql, direIds.toArray(), rs -> {
            Object flag = rs.getObject("delivered");
            out.put(rs.getLong("dire_id"), flag == null ? null : rs.getBoolean("delivered"));
        });
        return out;
    }

    public void savePayment(long direId, DanPaymentDto dto) {
        // delivery_status keeps only delivery state — payments go to payment_details
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM delivery_status WHERE dire_id=?", Integer.class, direId);
        if (count != null && count > 0) {
            jdbc.update("""
                    UPDATE delivery_status
                       SET delivered=?, reason=?
                     WHERE dire_id=?
                    """, dto.isDelivered(), dto.getReason(), direId);
        } else {
            jdbc.update("""
                    INSERT INTO delivery_status
                      (dire_id, delivered, reason, delivery_date)
                    VALUES (?,?,?,CURRENT_DATE)
                    """, direId, dto.isDelivered(), dto.getReason());
        }

        deliveryRepository.upsertPaymentDetails(direId,
                com.api.distr.docs.sales.dto.DeliveryStatusDTO.parsePaymentModes(
                        dto.getPaymentMode(), dto.getPaymentAmount()));

        log.info("savePayment: direId={}, delivered={}", direId, dto.isDelivered());
    }

    // ── Submit DAN ────────────────────────────────────────────────────────────

    public void submitDan(Long danId) {
        int updated = jdbc.update(
                "UPDATE dayend_approval SET status=" + com.api.distr.docs.dayend.DayEndStatus.CLOSED + ", approved_at=NOW() " +
                "WHERE id=? AND status IN (" + com.api.distr.docs.dayend.DayEndStatus.STARTED + "," +
                com.api.distr.docs.dayend.DayEndStatus.PENDING + "," + com.api.distr.docs.dayend.DayEndStatus.APPROVED + ")",
                danId);
        if (updated == 0)
            throw new IllegalStateException("DAN " + danId + " not found or already closed");

        // Delete failed (status=1) assignments for this DAN's agent
        int deleted = jdbc.update("""
                DELETE FROM delivery_assignments
                 WHERE delivery_boy_id = (SELECT delivery_id::text FROM dayend_approval WHERE id = ?)
                   AND status = 1
                """, danId);

        // Close remaining assignments with status 10
        int closed = jdbc.update("""
                UPDATE delivery_assignments
                   SET status = 10
                 WHERE delivery_boy_id = (SELECT delivery_id::text FROM dayend_approval WHERE id = ?)
                   AND status != 10
                """, danId);
        approvalRepository.logEvent(danId, "CLOSED", "CLOSED", currentUser(),
                "DAN closed — " + closed + " assignment(s) closed, " + deleted + " failed removed");
        log.info("submitDan: danId={}, deletedFailed={}, closedAssignments={}", danId, deleted, closed);
    }

    /** Authenticated username, or "system" when unauthenticated. */
    private String currentUser() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null
                    && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
                    && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) { }
        return "system";
    }

    // ── Web: save returns by dire_id (DanClosePage) ───────────────────────────

    /**
     * Save return rows for a specific dire_id (web frontend).
     * Resolves delivery_id from delivery_assignments via stage_sales_entery.
     */
    public long saveReturnsByDireId(long direId, List<WebReturnItemDto> items) {
        long deliveryId = 0L;
        try {
            java.util.Map<String, Object> info = jdbc.queryForMap(
                    "SELECT COALESCE(NULLIF(da.delivery_boy_id,'')::bigint, 0) AS delivery_id " +
                    "FROM stage_sales_entery sse " +
                    "LEFT JOIN delivery_assignments da ON da.dire_id = sse.dire_id " +
                    "WHERE sse.dire_id = ? LIMIT 1", direId);
            deliveryId = ((Number) info.get("delivery_id")).longValue();
        } catch (Exception e) {
            log.warn("saveReturnsByDireId: could not resolve deliveryId for direId={}, using 0", direId);
        }

        List<DanReturnDto> rows = new java.util.ArrayList<>();
        if (items != null) {
            for (WebReturnItemDto it : items) {
                if (it.getReturnQty() <= 0 && it.getReturnAmt() <= 0) continue;
                DanReturnDto dto = new DanReturnDto();
                dto.setSerial(it.getSerial()           != null ? it.getSerial()           : "");
                dto.setDescription(it.getDescription() != null ? it.getDescription()      : "");
                dto.setBillQty((int) it.getBillQty());
                dto.setBillAmt(it.getBillAmt());
                dto.setReturnQty((int) it.getReturnQty());
                dto.setReturnAmt(it.getReturnAmt());
                dto.setReason(it.getReason()           != null ? it.getReason()            : "");
                dto.setCustom(it.isCustom());
                rows.add(dto);
            }
        }

        return saveReturns(deliveryId, direId, "partial", rows);
    }

    // ── DAN Close Report ──────────────────────────────────────────────────────

    /** List rows for the DAN Close Report — optional date range + agent filters. */
    public List<com.api.distr.docs.dan.dto.DanReportRowDto> getReport(LocalDate fromDate, LocalDate toDate, Long agentId) {
        StringBuilder sql = new StringBuilder("""
            SELECT da.id                                    AS dan_id,
                   da.delivery_id,
                   da.delivery_date,
                   da.status,
                   COALESCE(dm.delivery_name, 'Agent ' || da.delivery_id) AS agent_name,
                   (SELECT COUNT(*) FROM delivery_status ds
                     WHERE ds.delivery_id = da.delivery_id
                       AND ds.delivery_date::date = da.delivery_date)     AS deliveries,
                   COALESCE((SELECT SUM(CAST(sse.net_value AS double precision))
                     FROM delivery_status ds
                     JOIN stage_sales_entery sse ON sse.dire_id = ds.dire_id
                     WHERE ds.delivery_id = da.delivery_id
                       AND ds.delivery_date::date = da.delivery_date), 0) AS amount,
                   COALESCE((SELECT SUM(dr.return_amt) FROM dan_returns dr
                     WHERE dr.delivery_id = da.delivery_id
                       AND DATE(dr.created_at) = da.delivery_date), 0)    AS returns_amt
            FROM dayend_approval da
            LEFT JOIN delivery_master dm ON dm.delivery_id = da.delivery_id
            WHERE 1=1
            """);
        List<Object> params = new java.util.ArrayList<>();
        if (fromDate != null) { sql.append(" AND da.delivery_date >= ?"); params.add(fromDate); }
        if (toDate != null)   { sql.append(" AND da.delivery_date <= ?"); params.add(toDate); }
        if (agentId != null)  { sql.append(" AND da.delivery_id = ?");    params.add(agentId); }
        sql.append(" ORDER BY da.delivery_date DESC, da.id DESC");

        return jdbc.query(sql.toString(), params.toArray(), (rs, rn) -> {
            com.api.distr.docs.dan.dto.DanReportRowDto dto = new com.api.distr.docs.dan.dto.DanReportRowDto();
            long danId = rs.getLong("dan_id");
            LocalDate d = rs.getDate("delivery_date").toLocalDate();
            dto.setDanId(danId);
            dto.setDanCode(buildDanCode(d, danId));
            dto.setDate(d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            dto.setAgentName(rs.getString("agent_name"));
            dto.setAgentCode("DA-" + rs.getLong("delivery_id"));
            dto.setDeliveries(rs.getInt("deliveries"));
            dto.setAmount(rs.getDouble("amount"));
            dto.setReturnsAmt(rs.getDouble("returns_amt"));
            dto.setStatus(rs.getInt("status") == com.api.distr.docs.dayend.DayEndStatus.CLOSED ? "Closed" : "Pending");
            return dto;
        });
    }

    /** Detail view of a single DAN: totals + invoice list with payment breakdown. */
    public com.api.distr.docs.dan.dto.DanReportDetailDto getReportDetail(long danId) {
        java.util.Map<String, Object> head = jdbc.queryForMap("""
            SELECT da.id, da.delivery_id, da.delivery_date, da.status,
                   COALESCE(dm.delivery_name, 'Agent ' || da.delivery_id) AS agent_name
            FROM dayend_approval da
            LEFT JOIN delivery_master dm ON dm.delivery_id = da.delivery_id
            WHERE da.id = ?
            """, danId);

        long      deliveryId = ((Number) head.get("delivery_id")).longValue();
        LocalDate date       = ((java.sql.Date) head.get("delivery_date")).toLocalDate();
        int       status     = ((Number) head.get("status")).intValue();

        com.api.distr.docs.dan.dto.DanReportDetailDto dto = new com.api.distr.docs.dan.dto.DanReportDetailDto();
        dto.setDanId(danId);
        dto.setDanCode(buildDanCode(date, danId));
        dto.setDate(date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dto.setAgentName(String.valueOf(head.get("agent_name")));
        dto.setAgentCode("DA-" + deliveryId);
        dto.setStatus(status == com.api.distr.docs.dayend.DayEndStatus.CLOSED ? "Closed" : "Pending");

        // Per-dire return items for this agent + date
        java.util.Map<Long, Double> returnsByDire = new java.util.HashMap<>();
        java.util.Map<Long, List<com.api.distr.docs.dan.dto.DanReportDetailDto.ReturnItem>> returnItemsByDire =
                new java.util.HashMap<>();
        jdbc.query("""
            SELECT dire_id, description, return_qty, return_amt, reason
            FROM dan_returns
            WHERE delivery_id = ? AND DATE(created_at) = ?
            ORDER BY dire_id, id
            """, new Object[]{deliveryId, date}, rs -> {
                long dire = rs.getLong("dire_id");
                returnsByDire.merge(dire, rs.getDouble("return_amt"), Double::sum);
                var item = new com.api.distr.docs.dan.dto.DanReportDetailDto.ReturnItem();
                item.setDescription(rs.getString("description"));
                item.setQty(rs.getInt("return_qty"));
                item.setAmount(rs.getDouble("return_amt"));
                item.setReason(rs.getString("reason"));
                returnItemsByDire.computeIfAbsent(dire, k -> new java.util.ArrayList<>()).add(item);
            });

        List<com.api.distr.docs.dan.dto.DanReportDetailDto.InvoiceRow> invoices = jdbc.query("""
            SELECT ds.dire_id,
                   COALESCE(sse.sales_order_no, '')                     AS invoice_no,
                   COALESCE(sse.cust_desc, '')                          AS cust_name,
                   COALESCE(CAST(sse.net_value AS double precision), 0) AS net_value,
            """ + com.api.distr.docs.sales.dto.PaymentDetailsUtil.COLS + """
            FROM delivery_status ds
            LEFT JOIN payment_details pd ON pd.dire_id = ds.dire_id
            LEFT JOIN stage_sales_entery sse ON sse.dire_id = ds.dire_id
            WHERE ds.delivery_id = ? AND ds.delivery_date::date = ?
            ORDER BY sse.sales_order_no
            """, new Object[]{deliveryId, date}, (rs, rn) -> {
                var row = new com.api.distr.docs.dan.dto.DanReportDetailDto.InvoiceRow();
                row.setDireId(rs.getLong("dire_id"));
                row.setInvoiceNo(rs.getString("invoice_no"));
                row.setCustName(rs.getString("cust_name"));
                row.setAmount(rs.getDouble("net_value"));
                row.setPaidAmount(rs.getDouble("pd_total"));
                row.setReturnAmt(returnsByDire.getOrDefault(rs.getLong("dire_id"), 0.0));
                row.setReturns(returnItemsByDire.getOrDefault(rs.getLong("dire_id"), List.of()));
                for (var pm : com.api.distr.docs.sales.dto.PaymentDetailsUtil.fromResultSet(rs)) {
                    var entry = new com.api.distr.docs.dan.dto.DanReportDetailDto.PaymentEntry(
                            pm.getMode(), pm.getAmount());
                    entry.setChequeNo(pm.getChequeNo());
                    entry.setBankName(pm.getBankName());
                    entry.setReferenceNo(pm.getReferenceNo());
                    row.getPayments().add(entry);
                }
                return row;
            });

        dto.setInvoices(invoices);
        dto.setDeliveries(invoices.size());
        dto.setTotalAmount(invoices.stream().mapToDouble(
                com.api.distr.docs.dan.dto.DanReportDetailDto.InvoiceRow::getAmount).sum());
        dto.setReturnsAmt(invoices.stream().mapToDouble(
                com.api.distr.docs.dan.dto.DanReportDetailDto.InvoiceRow::getReturnAmt).sum());
        dto.setReturnsCount((int) invoices.stream().filter(i -> i.getReturnAmt() > 0).count());
        dto.setNetSettled(dto.getTotalAmount() - dto.getReturnsAmt());
        return dto;
    }

    /** Per-mode payment totals across a set of dire_ids (from payment_details). */
    public java.util.Map<String, Double> getPaymentTotals(List<Long> direIds) {
        java.util.Map<String, Double> totals = new java.util.LinkedHashMap<>();
        for (String k : List.of("cash", "upi", "cheque", "neft", "credit")) totals.put(k, 0.0);
        if (direIds == null || direIds.isEmpty()) return totals;

        String ph = direIds.stream().map(x -> "?").collect(Collectors.joining(","));
        jdbc.query("""
            SELECT COALESCE(SUM(cash_amount),   0) AS cash,
                   COALESCE(SUM(upi_amount),    0) AS upi,
                   COALESCE(SUM(cheque_amount), 0) AS cheque,
                   COALESCE(SUM(neft_amount),   0) AS neft,
                   COALESCE(SUM(credit_amount), 0) AS credit
            FROM payment_details WHERE dire_id IN (""" + ph + ")",
            direIds.toArray(), rs -> {
                totals.put("cash",   rs.getDouble("cash"));
                totals.put("upi",    rs.getDouble("upi"));
                totals.put("cheque", rs.getDouble("cheque"));
                totals.put("neft",   rs.getDouble("neft"));
                totals.put("credit", rs.getDouble("credit"));
            });
        return totals;
    }

    /** Total saved return amount across a set of dire_ids. */
    public double getReturnTotal(List<Long> direIds) {
        if (direIds == null || direIds.isEmpty()) return 0.0;
        String ph = direIds.stream().map(x -> "?").collect(Collectors.joining(","));
        Double v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(return_amt), 0) FROM dan_returns WHERE dire_id IN (" + ph + ")",
                direIds.toArray(), Double.class);
        return v != null ? v : 0.0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static DanReturnDto mapReturn(java.sql.ResultSet rs, int rn) throws java.sql.SQLException {
        DanReturnDto r = new DanReturnDto();
        r.setId(rs.getLong("id"));
        r.setDireId(rs.getLong("dire_id"));
        r.setSerial(rs.getString("serial"));
        r.setDescription(rs.getString("description"));
        r.setBillQty(rs.getInt("bill_qty"));
        r.setBillAmt(rs.getDouble("bill_amt"));
        r.setReturnQty(rs.getInt("return_qty"));
        r.setReturnAmt(rs.getDouble("return_amt"));
        r.setReason(rs.getString("reason"));
        r.setCustom(rs.getBoolean("is_custom"));
        return r;
    }

    private List<DanPicklistDto> loadPicklistsByDireIds(String direIdsStr) {
        if (direIdsStr == null || direIdsStr.isBlank()) return List.of();
        List<Long> ids = Arrays.stream(direIdsStr.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::parseLong).collect(Collectors.toList());
        if (ids.isEmpty()) return List.of();
        String ph = ids.stream().map(n -> "?").collect(Collectors.joining(","));
        return jdbc.query(
                "SELECT dire_id, sales_order_no, picklist_no, customer_no, cust_desc, net_value " +
                "FROM stage_sales_entery WHERE dire_id IN (" + ph + ") ORDER BY dire_id",
                ids.toArray(), (rs, rn) -> {
                    DanPicklistDto p = new DanPicklistDto();
                    p.setNo(rn + 1);
                    p.setDireId(rs.getLong("dire_id"));
                    p.setInvoiceNo(rs.getString("sales_order_no"));
                    p.setPicklistNo(rs.getString("picklist_no"));
                    p.setCustName(rs.getString("cust_desc"));
                    p.setCustNo(rs.getString("customer_no"));
                    p.setNetValue(rs.getDouble("net_value"));
                    return p;
                });
    }

    private static String buildDanCode(LocalDate date, long id) {
        return String.format("DAN-%d%02d%02d-%03d",
                date.getYear(), date.getMonthValue(), date.getDayOfMonth(), id);
    }
}

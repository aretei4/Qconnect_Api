package com.api.distr.docs.sales.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Payment info lives in the payment_details table (one row per dire_id, mode
 * amounts flattened into columns). This util centralises reading those columns
 * and re-serialising them for legacy consumers that still expect the JSON string.
 *
 * Usage:  SELECT ... , PaymentDetailsUtil.COLS + " FROM ... LEFT JOIN payment_details pd ON pd.dire_id = x.dire_id"
 */
public final class PaymentDetailsUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PaymentDetailsUtil() {}

    /** Column list (aliased pd_*) to append to a SELECT that joins payment_details pd. */
    public static final String COLS = """
            COALESCE(pd.total_amount, 0)   AS pd_total,
            COALESCE(pd.cash_amount, 0)    AS pd_cash,
            COALESCE(pd.upi_amount, 0)     AS pd_upi,
            pd.upi_ref_no                  AS pd_upi_ref,
            COALESCE(pd.cheque_amount, 0)  AS pd_cheque,
            pd.cheque_no                   AS pd_cheque_no,
            pd.cheque_bank                 AS pd_cheque_bank,
            COALESCE(pd.neft_amount, 0)    AS pd_neft,
            pd.neft_ref_no                 AS pd_neft_ref,
            COALESCE(pd.credit_amount, 0)  AS pd_credit
            """;

    /** Builds the mode-entry list from the pd_* columns of the current row. */
    public static List<PaymentModeEntry> fromResultSet(ResultSet rs) throws SQLException {
        List<PaymentModeEntry> modes = new ArrayList<>();
        if (rs.getDouble("pd_cash") > 0) {
            modes.add(new PaymentModeEntry("CASH", rs.getDouble("pd_cash")));
        }
        if (rs.getDouble("pd_upi") > 0) {
            PaymentModeEntry m = new PaymentModeEntry("UPI", rs.getDouble("pd_upi"));
            m.setReferenceNo(rs.getString("pd_upi_ref"));
            modes.add(m);
        }
        if (rs.getDouble("pd_cheque") > 0) {
            PaymentModeEntry m = new PaymentModeEntry("CHEQUE", rs.getDouble("pd_cheque"));
            m.setChequeNo(rs.getString("pd_cheque_no"));
            m.setBankName(rs.getString("pd_cheque_bank"));
            modes.add(m);
        }
        if (rs.getDouble("pd_neft") > 0) {
            PaymentModeEntry m = new PaymentModeEntry("NEFT", rs.getDouble("pd_neft"));
            m.setReferenceNo(rs.getString("pd_neft_ref"));
            modes.add(m);
        }
        if (rs.getDouble("pd_credit") > 0) {
            modes.add(new PaymentModeEntry("CREDIT", rs.getDouble("pd_credit")));
        }
        return modes;
    }

    /** JSON array string for legacy consumers that still carry payment_mode as a string. */
    public static String toJson(List<PaymentModeEntry> modes) {
        if (modes == null || modes.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(modes);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            for (PaymentModeEntry m : modes) {
                if (sb.length() > 0) sb.append(",");
                sb.append(m.getMode()).append(":").append(m.getAmount());
            }
            return sb.toString();
        }
    }
}

package com.api.distr.docs.sales.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.api.distr.docs.sales.dto.SalesEntry;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
public class SalesEntryRepository {

    private final JdbcTemplate jdbcTemplate;

    // Allow only safe column names
    private static final Set<String> ALLOWED_COLUMNS = Set.of(
        "picklist_no", "sales_order_no", "customer_no", "cust_desc",
        "sales_rep_no", "sales_rep_name", "route", "route_name",
        "billing_date", "warehouse", "net_value", "update_date", "bu_id"
    );

    public SalesEntryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int deleteByPicklistNos(List<String> picklistNos) {
        // Build  DELETE ... WHERE picklist_no IN (?,?,?)
        String placeholders = String.join(",", java.util.Collections.nCopies(picklistNos.size(), "?"));
        String sql = "DELETE FROM stage_sales_entery WHERE picklist_no IN (" + placeholders + ")";
        return jdbcTemplate.update(sql, picklistNos.toArray());
    }

    public List<SalesEntry> findByFilters(Map<String, String> filters) {
        // Build dynamic SQL
        StringBuilder sql = new StringBuilder(QueryConstants.SELECT_SALES);
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String column = entry.getKey().toLowerCase();
            String value = entry.getValue();

            if (!ALLOWED_COLUMNS.contains(column)) {
                throw new IllegalArgumentException("Invalid column name: " + column);
            }

          //  sql.append(" AND ").append(column).append(" = ?");
            //params.add(value);
        }

        return jdbcTemplate.query(sql.toString(), params.toArray(), new SalesEntryMapper());
    }

    // Map result to SalesEntry object
    private static class SalesEntryMapper implements RowMapper<SalesEntry> {
        @Override
        public SalesEntry mapRow(ResultSet rs, int rowNum) throws SQLException {
            SalesEntry entry = new SalesEntry();
            entry.setPicklistNo(rs.getString("Picklist_No"));
            entry.setSalesOrderNo(rs.getString("Sales_Order_no"));
            entry.setCustomerNo(rs.getString("Customer_no"));
            entry.setCustDesc(rs.getString("Cust_desc"));
            entry.setSalesRepNo(rs.getString("Sales_rep_no"));
            entry.setSalesRepName(rs.getString("Sales_Rep_Name"));
            entry.setRoute(rs.getString("Route"));
            entry.setRouteName(rs.getString("Route_Name"));
            entry.setBillingDate(rs.getDate("Billing_Date"));
            entry.setWarehouse(rs.getString("Warehouse"));
            entry.setNetValue(rs.getDouble("Net_Value"));
            entry.setUpdateDate(rs.getDate("Update_Date"));
            entry.setBuId(rs.getInt("Bu_id"));
            entry.setCompanyName(rs.getString("company_name"));
            return entry;
        }
    }
}

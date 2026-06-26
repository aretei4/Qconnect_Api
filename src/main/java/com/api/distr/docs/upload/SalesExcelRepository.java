package com.api.distr.docs.upload;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SalesExcelRepository {

    private final JdbcTemplate jdbc;

    public SalesExcelRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SQL_CHECK =
            "SELECT COUNT(*) FROM stage_sales_entery WHERE sales_order_no = ?";

    private static final String SQL_INSERT =
            "INSERT INTO stage_sales_entery (" +
                    "picklist_no, sales_order_no, customer_no, cust_desc, sales_rep_no, " +
                    "sales_rep_name, route, route_name, billing_date, warehouse, net_value, " +
                    "company_name, update_date, bu_id" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, 100)";

    private static final String SQL_UPDATE =
            "UPDATE stage_sales_entery SET " +
                    "picklist_no=?, customer_no=?, cust_desc=?, sales_rep_no=?, " +
                    "sales_rep_name=?, route=?, route_name=?, billing_date=?, warehouse=?, " +
                    "net_value=?, company_name=?, update_date=CURRENT_DATE " +
                    "WHERE sales_order_no=?";
    

    public int findCountBySalesOrder(String salesOrderNo) {
        return jdbc.queryForObject(SQL_CHECK, Integer.class, salesOrderNo);
    }

    public void insert(SalesRecord r) {
        jdbc.update(con -> {
            var ps = con.prepareStatement(SQL_INSERT);
            ps.setString(1, r.getPicklistNo());
            ps.setString(2, r.getSalesOrderNo());
            ps.setString(3, r.getCustomerNo());
            ps.setString(4, r.getCustDesc());
            ps.setString(5, r.getSalesRepNo());
            ps.setString(6, r.getSalesRepName());
            ps.setString(7, r.getRoute());
            ps.setString(8, r.getRouteName());
            ps.setDate(9, r.getBillingDate());
            ps.setString(10, r.getWarehouse());
            ps.setDouble(11, r.getNetValue());
            ps.setString(12, r.getCompanyName());
            return ps;
        });
    }

    public void update(SalesRecord r) {
        jdbc.update(con -> {
            var ps = con.prepareStatement(SQL_UPDATE);
            ps.setString(1, r.getPicklistNo());    // SET picklist_no
            ps.setString(2, r.getCustomerNo());
            ps.setString(3, r.getCustDesc());
            ps.setString(4, r.getSalesRepNo());
            ps.setString(5, r.getSalesRepName());
            ps.setString(6, r.getRoute());
            ps.setString(7, r.getRouteName());
            ps.setDate(8, r.getBillingDate());
            ps.setString(9, r.getWarehouse());
            ps.setDouble(10, r.getNetValue());
            ps.setString(11, r.getCompanyName());
            ps.setString(12, r.getSalesOrderNo()); // WHERE sales_order_no
            return ps;
        });
    }
}


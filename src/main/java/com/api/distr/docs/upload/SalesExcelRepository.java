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
            "SELECT COUNT(*) FROM stage_sales_entery WHERE picklist_no = ?";

    private static final String SQL_INSERT =
            "INSERT INTO stage_sales_entery (" +
                    "picklist_no, sales_order_no, customer_no, cust_desc, sales_rep_no, " +
                    "sales_rep_name, route, route_name, billing_date, warehouse, net_value" +
            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE stage_sales_entery SET " +
                    "sales_order_no=?, customer_no=?, cust_desc=?, sales_rep_no=?, " +
                    "sales_rep_name=?, route=?, route_name=?, billing_date=?, warehouse=?, net_value=? " +
                    "WHERE picklist_no=?";
    

    public int findCountByPicklist(String picklistNo) {
        return jdbc.queryForObject(SQL_CHECK, Integer.class, picklistNo);
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
            return ps;
        });
    }

    public void update(SalesRecord r) {
        jdbc.update(con -> {
            var ps = con.prepareStatement(SQL_UPDATE);
            ps.setString(1, r.getSalesOrderNo());
            ps.setString(2, r.getCustomerNo());
            ps.setString(3, r.getCustDesc());
            ps.setString(4, r.getSalesRepNo());
            ps.setString(5, r.getSalesRepName());
            ps.setString(6, r.getRoute());
            ps.setString(7, r.getRouteName());
            ps.setDate(8, r.getBillingDate());
            ps.setString(9, r.getWarehouse());
            ps.setDouble(10, r.getNetValue());
            ps.setString(11, r.getPicklistNo());
            return ps;
        });
    }
}


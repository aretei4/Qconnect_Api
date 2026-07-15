package com.api.distr.docs.sales.repo;

public interface QueryConstants {

    String SELECT_SALES = """
            SELECT * FROM stage_sales_entery
            WHERE dire_id NOT IN (
                SELECT dire_id FROM delivery_assignments WHERE dire_id IS NOT NULL
            )""";

    String SELECT_DELIVERY = SELECT_SALES;

    String DELETE_DELIVERY_ASSIGN = "DELETE FROM delivery_assignments WHERE dire_id = ?";

    String SELECT_DELIVERY_ASSIGN =
            "SELECT " +
            "   sse.dire_id        AS direId, " +
            "   sse.sales_order_no AS invoiceNo, " +
            "   sse.picklist_no    AS picklistNo, " +
            "   sse.customer_no    AS customerNo, " +
            "   sse.cust_desc      AS custDesc, " +
            "   cd.cust_mobile     AS custMobile, " +
            "   sse.sales_rep_no   AS salesRepNo, " +
            "   sse.sales_rep_name AS salesRepName, " +
            "   sse.route, " +
            "   sse.route_name     AS routeName, " +
            "   sse.billing_date   AS billingDate, " +
            "   sse.warehouse, " +
            "   sse.net_value      AS netValue, " +
            "   sse.update_date    AS updateDate, " +
            "   sse.bu_id          AS buId, " +
            "   da.status          AS assignStatus " +
            "FROM stage_sales_entery sse " +
            "JOIN delivery_assignments da ON da.dire_id = sse.dire_id " +
            "LEFT JOIN customer_details cd ON cd.cust_no = sse.customer_no " +
            "WHERE da.status IN (0, 9) AND da.delivery_boy_id = '";

    // Base query without status filter — append status condition + delivery_boy_id before executing
    String SELECT_DELIVERY_ASSIGN_BASE =
            "SELECT " +
            "   sse.dire_id        AS direId, " +
            "   sse.sales_order_no AS invoiceNo, " +
            "   sse.picklist_no    AS picklistNo, " +
            "   sse.customer_no    AS customerNo, " +
            "   sse.cust_desc      AS custDesc, " +
            "   cd.cust_mobile     AS custMobile, " +
            "   sse.sales_rep_no   AS salesRepNo, " +
            "   sse.sales_rep_name AS salesRepName, " +
            "   sse.route, " +
            "   sse.route_name     AS routeName, " +
            "   sse.billing_date   AS billingDate, " +
            "   sse.warehouse, " +
            "   sse.net_value      AS netValue, " +
            "   sse.update_date    AS updateDate, " +
            "   sse.bu_id          AS buId, " +
            "   da.status          AS assignStatus " +
            "FROM stage_sales_entery sse " +
            "JOIN delivery_assignments da ON da.dire_id = sse.dire_id " +
            "LEFT JOIN customer_details cd ON cd.cust_no = sse.customer_no " +
            "WHERE da.delivery_boy_id = ? AND ";

    String SELECT_DELIVERY_MOBILE = """
            SELECT delivery_id, delivery_name, bu_id
            FROM delivery_master
            WHERE delivery_mobile = ?
              AND active = true
            """;
}

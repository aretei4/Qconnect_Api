package com.api.distr.docs.sales.repo;

public interface QueryConstants {

	String SELECT_SALES = "SELECT * FROM stage_sales_entery \r\n"
			+ "WHERE Picklist_No not in(select Picklist_No from delivery_assignments)";
	String SELECT_DELIVERY = "SELECT * FROM stage_sales_entery \r\n"
			+ "WHERE Picklist_No not in(select Picklist_No from delivery_assignments)";
	String DELETE_DELIVERY_ASSIGN = "delete FROM delivery_assignments where picklist_no=?";
	 String SELECT_DELIVERY_ASSIGN = 
	        "SELECT " +
	        "   picklist_no AS picklistNo, " +
	        "   sales_order_no AS salesOrderNo, " +
	        "   customer_no AS customerNo, " +
	        "   cust_desc AS custDesc, " +
	        "   sales_rep_no AS salesRepNo, " +
	        "   sales_rep_name AS salesRepName, " +
	        "   route, " +
	        "   route_name AS routeName, " +
	        "   billing_date AS billingDate, " +
	        "   warehouse, " +
	        "   net_value AS netValue, " +
	        "   update_date AS updateDate, " +
	        "   bu_id AS buId " +
	        "FROM stage_sales_entery " +
	        "WHERE picklist_no IN (" +
	        "SELECT picklist_no FROM delivery_assignments where status=0 and delivery_boy_id ='";
	 
	 String SELECT_DELIVERY_MOBILE = """
	            SELECT delivery_id, delivery_name, bu_id
	            FROM delivery_master
	            WHERE delivery_mobile = ?
	              AND active = true
	        """;

}

package com.api.distr.docs.jwt;

import java.util.List;

public interface DistrConstants {
	
	static final List<String> REQUIRED_HEADERS = List.of("Name", "Score", "Department");
	static final String DATE_FORMAT = "DD/mm/yyyy";
	static final String STAGE_SELECT_COUNT = 
		    "SELECT COUNT(*) FROM stage_sales_entery WHERE picklist_no = ?";

		static final String STAGE_INSERT = 
		    "INSERT INTO stage_sales_entery (" +
		    "picklist_no, sales_order_no, customer_no, cust_desc, " +
		    "sales_rep_no, sales_rep_name, route, route_name, billing_date, warehouse, net_value, update_date, bu_id" +
		    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, 100)";

		static final String STAGE_UPDATE = 
		    "UPDATE stage_sales_entery SET " +
		    "sales_order_no = ?, customer_no = ?, cust_desc = ?, " +
		    "sales_rep_no = ?, sales_rep_name = ?, route = ?, route_name = ?, " +
		    "billing_date = ?, warehouse = ?, net_value = ?, update_date = CURRENT_DATE " +
		    "WHERE picklist_no = ?";


}

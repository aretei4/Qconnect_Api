package com.api.distr.docs.jwt;

import java.util.List;

public interface DistrConstants {
	
	static final List<String> REQUIRED_HEADERS = List.of("Name", "Score", "Department");
	static final String DATE_FORMAT = "DD/mm/yyyy";
	static final String STAGE_SELECT_COUNT = "SELECT COUNT(*) FROM Stage_Sales_Entery WHERE \"Picklist_No\" = ?";
	static final String STAGE_INSERT = "INSERT INTO Stage_Sales_Entery (\"Picklist_No\", \"Sales_Order_no\", \"Customer_no\", \"Cust_desc\", " +
										"\"Sales_rep_no\", \"Sales_Rep_Name\", \"Route\", \"Route_Name\", \"Billing_Date\", \"Warehouse\", \"Net_Value\", \"Update_Date\", \"Bu_id\") " +
										"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_DATE, 100)";
	static final String STAGE_UPDATE = "UPDATE Stage_Sales_Entery SET \"Sales_Order_no\" = ?, \"Customer_no\" = ?, \"Cust_desc\" = ?, " +
										"\"Sales_rep_no\" = ?, \"Sales_Rep_Name\" = ?, \"Route\" = ?, \"Route_Name\" = ?, " +
										"\"Billing_Date\" = ?, \"Warehouse\" = ?, \"Net_Value\" = ?, \"Update_Date\" = CURRENT_DATE " +
										"WHERE \"Picklist_No\" = ?";

}

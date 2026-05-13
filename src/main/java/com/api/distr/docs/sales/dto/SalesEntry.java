package com.api.distr.docs.sales.dto;


import java.sql.Date;

public class SalesEntry {
    private String picklistNo;
    private String salesOrderNo;
    private String customerNo;
    private String custDesc;
    private String salesRepNo;
    private String salesRepName;
    private String route;
    private String routeName;
    private Date billingDate;
    private String warehouse;
    private Double netValue;
    private Date updateDate;
    private Integer buId;
    private String companyName;

    // Getters & Setters
    public String getPicklistNo() { return picklistNo; }
    public void setPicklistNo(String picklistNo) { this.picklistNo = picklistNo; }

    public String getSalesOrderNo() { return salesOrderNo; }
    public void setSalesOrderNo(String salesOrderNo) { this.salesOrderNo = salesOrderNo; }

    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }

    public String getCustDesc() { return custDesc; }
    public void setCustDesc(String custDesc) { this.custDesc = custDesc; }

    public String getSalesRepNo() { return salesRepNo; }
    public void setSalesRepNo(String salesRepNo) { this.salesRepNo = salesRepNo; }

    public String getSalesRepName() { return salesRepName; }
    public void setSalesRepName(String salesRepName) { this.salesRepName = salesRepName; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public Date getBillingDate() { return billingDate; }
    public void setBillingDate(Date billingDate) { this.billingDate = billingDate; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }

    public Double getNetValue() { return netValue; }
    public void setNetValue(Double netValue) { this.netValue = netValue; }

    public Date getUpdateDate() { return updateDate; }
    public void setUpdateDate(Date updateDate) { this.updateDate = updateDate; }

    public Integer getBuId() { return buId; }
    public void setBuId(Integer buId) { this.buId = buId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}


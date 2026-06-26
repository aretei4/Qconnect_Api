package com.api.distr.docs.dashboard;


public class DeliverySummaryDto {

    private int    totalDeliveries;
    private int    delivered;
    private int    pending;
    private int    cancelled;

    private int    todayTotal = 30;
    private int    todayDelivered;
    private int    todayPending;
    private int    todayCancelled;

    // Net invoice value (from stage_sales_entery)
    private double todayNetValue;
    private double totalNetValue;

    // Payment collected (from delivery_status)
    private double todayCollected;
    private double totalCollected;

    public double getTodayNetValue()           { return todayNetValue; }
    public void   setTodayNetValue(double v)   { this.todayNetValue = v; }

    public double getTotalNetValue()           { return totalNetValue; }
    public void   setTotalNetValue(double v)   { this.totalNetValue = v; }

    public double getTodayCollected()          { return todayCollected; }
    public void   setTodayCollected(double v)  { this.todayCollected = v; }

    public double getTotalCollected()          { return totalCollected; }
    public void   setTotalCollected(double v)  { this.totalCollected = v; }
    
    
    public int getTodayTotal() {
		return todayTotal;
	}

	public void setTodayTotal(int todayTotal) {
		this.todayTotal = todayTotal;
	}

	public int getTodayDelivered() {
		return todayDelivered;
	}

	public void setTodayDelivered(int todayDelivered) {
		this.todayDelivered = todayDelivered;
	}

	public int getTodayPending() {
		return todayPending;
	}

	public void setTodayPending(int todayPending) {
		this.todayPending = todayPending;
	}

	public int getTodayCancelled() {
		return todayCancelled;
	}

	public void setTodayCancelled(int todayCancelled) {
		this.todayCancelled = todayCancelled;
	}

	public int getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(int totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public int getDelivered() {
        return delivered;
    }

    public void setDelivered(int delivered) {
        this.delivered = delivered;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public int getCancelled() {
        return cancelled;
    }

    public void setCancelled(int cancelled) {
        this.cancelled = cancelled;
    }
}


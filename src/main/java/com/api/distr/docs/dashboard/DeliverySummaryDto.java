package com.api.distr.docs.dashboard;


public class DeliverySummaryDto {

    private int totalDeliveries;
    private int delivered;
    private int pending;
    private int cancelled;

    private int todayTotal=30;
    private int todayDelivered;
    private int todayPending;
    private int todayCancelled;
    
    
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


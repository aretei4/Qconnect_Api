package com.api.distr.docs.dashboard;

/** Overall summary — CLOSED (status 10) deliveries only. */
public class OverallSummaryDto {

    private int    closed;
    private double closedValue;
    private double closedCollected;

    public int    getClosed()                  { return closed; }
    public void   setClosed(int v)             { this.closed = v; }

    public double getClosedValue()             { return closedValue; }
    public void   setClosedValue(double v)     { this.closedValue = v; }

    public double getClosedCollected()         { return closedCollected; }
    public void   setClosedCollected(double v) { this.closedCollected = v; }
}

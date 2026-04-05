package com.smartit.model;

/**
 * DashboardTrendPoint — simple label/value pair for dashboard chart rows.
 */
public class DashboardTrendPoint {

    private final String label;
    private final int value;

    public DashboardTrendPoint(String label, int value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }
}

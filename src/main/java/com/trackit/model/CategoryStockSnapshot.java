package com.trackit.model;

/**
 * CategoryStockSnapshot — grouped stock figures for one item category.
 */
public class CategoryStockSnapshot {

    private final String category;
    private final int totalUnits;
    private final int reservedUnits;
    private final int availableUnits;

    public CategoryStockSnapshot(String category, int totalUnits, int reservedUnits, int availableUnits) {
        this.category = category;
        this.totalUnits = totalUnits;
        this.reservedUnits = reservedUnits;
        this.availableUnits = availableUnits;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public int getReservedUnits() {
        return reservedUnits;
    }

    public int getAvailableUnits() {
        return availableUnits;
    }
}


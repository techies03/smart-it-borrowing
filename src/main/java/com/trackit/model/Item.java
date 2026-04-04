package com.trackit.model;


/**
 * Item — Entity model for IT equipment.
 * Maps to the `items` table.
 */
public class Item {

    private int       id;
    private String    name;
    private String    category;
    private int       quantity;
    private String    status;         // "AVAILABLE" | "UNAVAILABLE"
    private String    itemCondition;  // "GOOD" | "DAMAGED"
    private String    imageUrl;
    private int       unitsInUse;

    // --- Constructors ---

    public Item() {}

    public Item(int id, String name, String category, int quantity,
                String status, String itemCondition, String imageUrl) {
        this.id            = id;
        this.name          = name;
        this.category      = category;
        this.quantity      = quantity;
        this.status        = status;
        this.itemCondition = itemCondition;
        this.imageUrl      = imageUrl;
    }

    // --- Getters & Setters ---

    public int    getId()         { return id; }
    public void   setId(int id)   { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getCategory()                 { return category; }
    public void   setCategory(String category)  { this.category = category; }

    public int    getQuantity()               { return quantity; }
    public void   setQuantity(int quantity)   { this.quantity = quantity; }

    public String getStatus()               { return status; }
    public void   setStatus(String status)  { this.status = status; }

    public String getItemCondition()                     { return itemCondition; }
    public void   setItemCondition(String itemCondition) { this.itemCondition = itemCondition; }

    public String getImageUrl()                { return imageUrl; }
    public void   setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int    getUnitsInUse()               { return unitsInUse; }
    public void   setUnitsInUse(int unitsInUse) { this.unitsInUse = Math.max(0, unitsInUse); }

    public int    getReservedUnits()                 { return unitsInUse; }
    public void   setReservedUnits(int reservedUnits) { setUnitsInUse(reservedUnits); }

    public int getUnitsAvailableNow() {
        return Math.max(quantity - unitsInUse, 0);
    }

    public int getAvailableUnits() {
        return getUnitsAvailableNow();
    }

    /** Convenience: returns true if status is AVAILABLE */
    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    /** True when the item can still accept new booking requests. */
    public boolean isRequestable() {
        return isAvailable() && quantity > 0;
    }

    /** True when at least one unit is free right now. */
    public boolean hasUnitsAvailableNow() {
        return isRequestable() && getUnitsAvailableNow() > 0;
    }

    public boolean hasAvailableUnits() {
        return hasUnitsAvailableNow();
    }

    @Override
    public String toString() {
        return "Item{id=" + id + ", name='" + name + "', category='" + category
                + "', quantity=" + quantity + ", status='" + status + "'}";
    }
}


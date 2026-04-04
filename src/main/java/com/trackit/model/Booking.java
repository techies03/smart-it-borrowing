package com.trackit.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Booking — Entity model for borrow requests.
 * Maps to the `bookings` table.
 * Includes join fields (userName, itemName) for display — not stored in DB.
 */
public class Booking {

    private int        id;
    private int        userId;
    private int        itemId;
    private int        quantity;

    private LocalDateTime borrowDate;
    private LocalDateTime returnDate;        // expected return datetime
    private LocalDateTime actualReturnDate;  // actual return datetime (null until returned)

    private String     status;            // PENDING | APPROVED | REJECTED | RETURN_PENDING | RETURNED
    private BigDecimal penalty;           // RM5 per day late

    private String     conditionBefore;   // GOOD | DAMAGED (at borrow time)
    private String     conditionAfter;    // GOOD | DAMAGED (at return time, null until returned)

    // ---- Display-only fields (populated via SQL JOIN, not stored) ----
    private String     userName;
    private String     itemName;

    // --- Constructors ---

    public Booking() {
        this.quantity = 1;
        this.penalty = BigDecimal.ZERO;
    }

    // --- Getters & Setters ---

    public int       getId()          { return id; }
    public void      setId(int id)    { this.id = id; }

    public int       getUserId()              { return userId; }
    public void      setUserId(int userId)    { this.userId = userId; }

    public int       getItemId()              { return itemId; }
    public void      setItemId(int itemId)    { this.itemId = itemId; }

    public int       getQuantity()                { return quantity; }
    public void      setQuantity(int quantity)    { this.quantity = quantity; }

    public LocalDateTime getBorrowDate()                        { return borrowDate; }
    public void          setBorrowDate(LocalDateTime borrowDate){ this.borrowDate = borrowDate; }

    public LocalDateTime getReturnDate()                        { return returnDate; }
    public void          setReturnDate(LocalDateTime returnDate){ this.returnDate = returnDate; }

    public LocalDateTime getActualReturnDate()                              { return actualReturnDate; }
    public void          setActualReturnDate(LocalDateTime actualReturnDate){ this.actualReturnDate = actualReturnDate; }

    public String    getStatus()               { return status; }
    public void      setStatus(String status)  { this.status = status; }

    public BigDecimal getPenalty()                  { return penalty; }
    public void       setPenalty(BigDecimal penalty){ this.penalty = penalty; }

    public String    getConditionBefore()                      { return conditionBefore; }
    public void      setConditionBefore(String conditionBefore){ this.conditionBefore = conditionBefore; }

    public String    getConditionAfter()                     { return conditionAfter; }
    public void      setConditionAfter(String conditionAfter){ this.conditionAfter = conditionAfter; }

    public String    getUserName()                 { return userName; }
    public void      setUserName(String userName)  { this.userName = userName; }

    public String    getItemName()                 { return itemName; }
    public void      setItemName(String itemName)  { this.itemName = itemName; }

    /** Convenience: true if status is APPROVED */
    public boolean isApproved()  { return "APPROVED".equalsIgnoreCase(status); }

    /** Convenience: true if status is RETURNED */
    public boolean isReturned()  { return "RETURNED".equalsIgnoreCase(status); }

    /** Convenience: true if status is awaiting admin return confirmation */
    public boolean isReturnPending() { return "RETURN_PENDING".equalsIgnoreCase(status); }

    /** Convenience: true if item was returned late (penalty > 0) */
    public boolean isLate() {
        return penalty != null && penalty.compareTo(BigDecimal.ZERO) > 0;
    }

    /** Stable display reference derived from the primary key. */
    public String getReferenceCode() {
        return id > 0 ? String.format("BK-%04d", id) : "BK-----";
    }

    @Override
    public String toString() {
        return "Booking{id=" + id + ", userId=" + userId + ", itemId=" + itemId
                + ", quantity=" + quantity + ", status='" + status + "', penalty=" + penalty + "}";
    }
}


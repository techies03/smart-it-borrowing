package com.smartit.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DashboardReportData — Snapshot used for dashboard exports.
 *
 * Booking metrics are filtered by the selected borrow-date range.
 * Inventory figures remain a current snapshot taken at export time.
 */
public class DashboardReportData {

    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final LocalDateTime generatedAt;
    private final int totalItems;
    private final int totalUnits;
    private final int availableUnits;
    private final int reservedUnits;
    private final int totalBookings;
    private final int activeBookings;
    private final int overdueBookings;
    private final int pendingBookings;
    private final int returnPendingBookings;
    private final int returnedBookings;
    private final int rejectedBookings;
    private final int totalUsers;
    private final int dueToday;
    private final List<DashboardTrendPoint> bookingTrend;
    private final List<CategoryStockSnapshot> categoryStock;
    private final List<Booking> bookings;

    public DashboardReportData(LocalDate fromDate,
                               LocalDate toDate,
                               LocalDateTime generatedAt,
                               int totalItems,
                               int totalUnits,
                               int availableUnits,
                               int reservedUnits,
                               int totalBookings,
                               int activeBookings,
                               int overdueBookings,
                               int pendingBookings,
                               int returnPendingBookings,
                               int returnedBookings,
                               int rejectedBookings,
                               int totalUsers,
                               int dueToday,
                               List<DashboardTrendPoint> bookingTrend,
                               List<CategoryStockSnapshot> categoryStock,
                               List<Booking> bookings) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.generatedAt = generatedAt;
        this.totalItems = totalItems;
        this.totalUnits = totalUnits;
        this.availableUnits = availableUnits;
        this.reservedUnits = reservedUnits;
        this.totalBookings = totalBookings;
        this.activeBookings = activeBookings;
        this.overdueBookings = overdueBookings;
        this.pendingBookings = pendingBookings;
        this.returnPendingBookings = returnPendingBookings;
        this.returnedBookings = returnedBookings;
        this.rejectedBookings = rejectedBookings;
        this.totalUsers = totalUsers;
        this.dueToday = dueToday;
        this.bookingTrend = List.copyOf(bookingTrend);
        this.categoryStock = List.copyOf(categoryStock);
        this.bookings = List.copyOf(bookings);
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public int getAvailableUnits() {
        return availableUnits;
    }

    public int getReservedUnits() {
        return reservedUnits;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public int getActiveBookings() {
        return activeBookings;
    }

    public int getOverdueBookings() {
        return overdueBookings;
    }

    public int getPendingBookings() {
        return pendingBookings;
    }

    public int getReturnPendingBookings() {
        return returnPendingBookings;
    }

    public int getReturnedBookings() {
        return returnedBookings;
    }

    public int getRejectedBookings() {
        return rejectedBookings;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public int getDueToday() {
        return dueToday;
    }

    public List<DashboardTrendPoint> getBookingTrend() {
        return bookingTrend;
    }

    public List<CategoryStockSnapshot> getCategoryStock() {
        return categoryStock;
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public int getApprovedBookings() {
        return Math.max(activeBookings - returnPendingBookings, 0);
    }
}

package com.trackit.service;

import com.trackit.dao.BookingDAO;
import com.trackit.dao.ItemDAO;
import com.trackit.model.Booking;
import com.trackit.model.Item;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BookingService — Core business logic for borrowing.
 *
 * Key responsibilities:
 *  1. Conflict detection (date overlap) before creating a booking
 *  2. Penalty calculation: RM 5.00 per overdue day on return
 *  3. Condition tracking: condition before borrow / after return
 */
public class BookingService {

    /** Late return fine per day, in RM */
    private static final BigDecimal PENALTY_PER_DAY = new BigDecimal("5.00");

    private final BookingDAO bookingDAO = new BookingDAO();
    private final ItemDAO    itemDAO    = new ItemDAO();

    /**
     * Create one or more booking rows (status = PENDING), one row per requested unit.
     *
     * @return number of booking entries created
     * @throws IllegalArgumentException if dates are invalid or the item has a conflict
     */
    public int createBookings(int userId, int itemId,
                              int requestedQuantity,
                              LocalDateTime borrowDate, LocalDateTime returnDate,
                              String conditionBefore) throws SQLException {

        // --- Validation ---
        if (borrowDate == null || returnDate == null) {
            throw new IllegalArgumentException("Borrow date and return date are required.");
        }
        if (returnDate.isBefore(borrowDate)) {
            throw new IllegalArgumentException("Return date must be on or after the borrow date.");
        }
        if (borrowDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Borrow date cannot be in the past.");
        }
        if (requestedQuantity < 1) {
            throw new IllegalArgumentException("Booking quantity must be at least 1.");
        }

        // --- Item existence check ---
        Optional<Item> itemOpt = itemDAO.findById(itemId);
        if (itemOpt.isEmpty()) {
            throw new IllegalArgumentException("Selected item does not exist.");
        }
        Item item = itemOpt.get();
        if (!item.isRequestable()) {
            throw new IllegalArgumentException("Item '" + item.getName() + "' is currently unavailable.");
        }
        if (requestedQuantity > item.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity exceeds the total stock for this item.");
        }

        // --- Stock-aware overlap detection ---
        int overlappingBookings = bookingDAO.countOverlappingOpenBookings(itemId, borrowDate, returnDate);
        int remainingUnits = Math.max(item.getQuantity() - overlappingBookings, 0);
        if (requestedQuantity > remainingUnits) {
            throw new IllegalArgumentException(
                "Only " + remainingUnits + " unit(s) of '" + item.getName()
                + "' are available for the selected dates.");
        }

        // --- Create booking ---
        String normalizedCondition = normalizeCondition(conditionBefore);
        List<Booking> bookings = new ArrayList<>(requestedQuantity);
        for (int i = 0; i < requestedQuantity; i++) {
            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setItemId(itemId);
            booking.setQuantity(1);
            booking.setBorrowDate(borrowDate);
            booking.setReturnDate(returnDate);
            booking.setConditionBefore(normalizedCondition);
            bookings.add(booking);
        }

        bookingDAO.createAll(bookings);
        return bookings.size();
    }

    /**
     * Approve or reject a pending booking (admin action).
     */
    public void updateStatus(int bookingId, String newStatus) throws SQLException {
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new IllegalArgumentException("Status must be APPROVED or REJECTED.");
        }
        if ("APPROVED".equals(newStatus)) {
            Booking booking = bookingDAO.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found."));
            Item item = itemDAO.findById(booking.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Booked item not found."));

            int reservedUnits = bookingDAO.countReservedUnitsForItem(item.getId());
            int remainingUnits = Math.max(item.getQuantity() - reservedUnits, 0);
            if (booking.getQuantity() > remainingUnits) {
                throw new IllegalArgumentException(
                        "Not enough stock remains to approve " + booking.getReferenceCode()
                        + ". Only " + remainingUnits + " unit(s) are still available.");
            }
        }
        bookingDAO.updateStatus(bookingId, newStatus);
    }

    /**
     * Submit a return request — admin must still inspect and confirm it.
     *
     * Penalty = max(0, daysLate) × RM5.00, based on the submitted return time.
     */
    public BigDecimal requestReturn(int bookingId, String conditionAfter) throws SQLException {
        Optional<Booking> bookingOpt = bookingDAO.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            throw new IllegalArgumentException("Booking not found.");
        }

        Booking booking = bookingOpt.get();
        if (!"APPROVED".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Only APPROVED bookings can be returned.");
        }

        LocalDateTime actualReturnDate = LocalDateTime.now();
        LocalDate expectedReturnDate   = booking.getReturnDate().toLocalDate();
        LocalDate actualReturnDay      = actualReturnDate.toLocalDate();

        // Penalty: RM5 per day if returned after expected date
        long daysLate = ChronoUnit.DAYS.between(expectedReturnDate, actualReturnDay);
        BigDecimal penalty = daysLate > 0
                ? PENALTY_PER_DAY.multiply(BigDecimal.valueOf(daysLate))
                : BigDecimal.ZERO;

        bookingDAO.requestReturn(bookingId, actualReturnDate, penalty, normalizeCondition(conditionAfter));
        return penalty;
    }

    /**
     * Admin confirms that a pending return has been physically received and checked.
     */
    public void confirmReturn(int bookingId, BigDecimal damageFee) throws SQLException {
        Booking booking = bookingDAO.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!"RETURN_PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Only RETURN_PENDING bookings can be confirmed.");
        }

        BigDecimal normalizedDamageFee = normalizeDamageFee(booking, damageFee);
        BigDecimal currentPenalty = booking.getPenalty() != null ? booking.getPenalty() : BigDecimal.ZERO;
        BigDecimal finalPenalty = currentPenalty.add(normalizedDamageFee);

        Item item = itemDAO.findById(booking.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Booked item not found."));
        applyInventoryStateAfterReturn(item, booking);
        itemDAO.update(item);

        bookingDAO.confirmReturn(bookingId, finalPenalty);
    }

    /**
     * Admin rejects a return request and sends it back to APPROVED.
     */
    public void rejectReturn(int bookingId) throws SQLException {
        Booking booking = bookingDAO.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (!"RETURN_PENDING".equalsIgnoreCase(booking.getStatus())) {
            throw new IllegalArgumentException("Only RETURN_PENDING bookings can be rejected.");
        }

        bookingDAO.rejectReturn(bookingId);
    }

    /** All bookings — admin view */
    public List<Booking> getAllBookings() throws SQLException {
        return bookingDAO.findAll();
    }

    /** Filtered bookings — admin view */
    public List<Booking> getAllBookingsFiltered(String keyword, String status) throws SQLException {
        return bookingDAO.findAllFiltered(normalizeKeywordLike(keyword), extractBookingId(keyword), normalizeStatus(status));
    }

    /** Bookings for a specific user */
    public List<Booking> getBookingsByUser(int userId) throws SQLException {
        return bookingDAO.findByUserId(userId);
    }

    /** Filtered bookings for a specific user */
    public List<Booking> getBookingsByUserFiltered(int userId, String keyword, String status) throws SQLException {
        return bookingDAO.findByUserIdFiltered(userId, normalizeKeywordLike(keyword), extractBookingId(keyword), normalizeStatus(status));
    }

    /** Single booking by ID */
    public Optional<Booking> getBookingById(int id) throws SQLException {
        return bookingDAO.findById(id);
    }

    /** Dashboard: total active bookings */
    public int countActive() throws SQLException {
        return bookingDAO.countActive();
    }

    /** Dashboard: total overdue bookings */
    public int countOverdue() throws SQLException {
        return bookingDAO.countOverdue();
    }

    private String normalizeCondition(String condition) {
        String normalized = condition == null || condition.isBlank()
                ? "GOOD"
                : condition.trim().toUpperCase();

        if (!"GOOD".equals(normalized) && !"DAMAGED".equals(normalized)) {
            throw new IllegalArgumentException("Condition must be GOOD or DAMAGED.");
        }

        return normalized;
    }

    private BigDecimal normalizeDamageFee(Booking booking, BigDecimal damageFee) {
        BigDecimal normalized = damageFee != null ? damageFee : BigDecimal.ZERO;
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Damage fee cannot be negative.");
        }

        boolean damageFeeRequired =
                "GOOD".equalsIgnoreCase(booking.getConditionBefore())
             && "DAMAGED".equalsIgnoreCase(booking.getConditionAfter());

        if (damageFeeRequired && normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Enter a damage fee when an item is returned damaged.");
        }

        if (!damageFeeRequired && normalized.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException(
                    "Damage fee can only be added when an item was borrowed in GOOD condition and returned DAMAGED.");
        }

        return normalized;
    }

    /**
     * Quantity represents borrowable units.
     * If one good unit comes back damaged, reduce usable stock instead of freezing the whole item.
     */
    private void applyInventoryStateAfterReturn(Item item, Booking booking) {
        String returnedCondition = normalizeCondition(booking.getConditionAfter());
        boolean newlyDamaged =
                "GOOD".equalsIgnoreCase(booking.getConditionBefore())
             && "DAMAGED".equalsIgnoreCase(returnedCondition);

        if (newlyDamaged && item.getQuantity() > booking.getQuantity()) {
            item.setQuantity(item.getQuantity() - booking.getQuantity());
            item.setItemCondition("GOOD");
            return;
        }

        if (newlyDamaged) {
            item.setQuantity(Math.max(item.getQuantity() - booking.getQuantity(), 0));
        }
        item.setItemCondition(returnedCondition);
        if (newlyDamaged) {
            item.setStatus("UNAVAILABLE");
        }
    }

    private String normalizeKeywordLike(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private Integer extractBookingId(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String normalized = keyword.trim().toUpperCase();
        if (normalized.startsWith("BK-")) {
            normalized = normalized.substring(3);
        }

        normalized = normalized.replaceFirst("^0+(?!$)", "");
        if (!normalized.matches("\\d+")) {
            return null;
        }

        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase();
    }
}


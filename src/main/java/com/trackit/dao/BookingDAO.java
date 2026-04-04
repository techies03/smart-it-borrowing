package com.trackit.dao;

import com.trackit.model.Booking;
import com.trackit.util.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * BookingDAO — Data Access Object for the `bookings` table.
 * Key features:
 *  - Conflict detection: hasConflict() checks date overlap
 *  - Dashboard counts: countActive(), countOverdue()
 *  - Join query: all bookings with user name + item name
 */
public class BookingDAO {

    /** SQL to fetch bookings with user/item names via JOIN */
    private static final String SELECT_WITH_NAMES =
        "SELECT b.id, b.user_id, b.item_id, b.quantity, b.borrow_date, b.return_date, " +
        "       b.actual_return_date, b.status, b.penalty, " +
        "       b.condition_before, b.condition_after, " +
        "       u.name AS user_name, i.name AS item_name " +
        "FROM bookings b " +
        "JOIN users u ON u.id = b.user_id " +
        "JOIN items i ON i.id = b.item_id ";

    /**
     * All bookings (admin view), newest first.
     */
    public List<Booking> findAll() throws SQLException {
        return query(SELECT_WITH_NAMES + "ORDER BY b.created_at DESC", List.of());
    }

    /**
     * Bookings belonging to a specific user.
     */
    public List<Booking> findByUserId(int userId) throws SQLException {
        return query(SELECT_WITH_NAMES + "WHERE b.user_id = ? ORDER BY b.created_at DESC",
                List.of(userId));
    }

    /**
     * Filtered bookings for admin listing.
     */
    public List<Booking> findAllFiltered(String keywordLike, Integer bookingId, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_WITH_NAMES).append("WHERE 1=1");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, keywordLike, bookingId, status, true);
        sql.append(" ORDER BY b.created_at DESC");
        return query(sql.toString(), params);
    }

    /**
     * Filtered bookings for a single user listing.
     */
    public List<Booking> findByUserIdFiltered(int userId, String keywordLike, Integer bookingId, String status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_WITH_NAMES).append("WHERE b.user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);
        appendFilters(sql, params, keywordLike, bookingId, status, false);
        sql.append(" ORDER BY b.created_at DESC");
        return query(sql.toString(), params);
    }

    /**
     * Single booking by primary key.
     */
    public Optional<Booking> findById(int id) throws SQLException {
        List<Booking> list = query(SELECT_WITH_NAMES + "WHERE b.id = ?", List.of(id));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Insert a new booking.
     * Generates and sets the ID on the provided object.
     */
    public void create(Booking b) throws SQLException {
        createAll(List.of(b));
    }

    /**
     * Insert multiple booking rows atomically.
     * Generates and sets the IDs on the provided objects.
     */
    public void createAll(List<Booking> bookings) throws SQLException {
        if (bookings == null || bookings.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO bookings " +
            "(user_id, item_id, quantity, borrow_date, return_date, status, condition_before) " +
            "VALUES (?, ?, ?, ?, ?, 'PENDING', ?)";

        try (Connection conn = DBConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                for (Booking booking : bookings) {
                    ps.setInt   (1, booking.getUserId());
                    ps.setInt   (2, booking.getItemId());
                    ps.setInt   (3, booking.getQuantity());
                    ps.setTimestamp(4, Timestamp.valueOf(booking.getBorrowDate()));
                    ps.setTimestamp(5, Timestamp.valueOf(booking.getReturnDate()));
                    ps.setString(6, booking.getConditionBefore());
                    ps.addBatch();
                }

                ps.executeBatch();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    Iterator<Booking> iterator = bookings.iterator();
                    while (keys.next() && iterator.hasNext()) {
                        iterator.next().setId(keys.getInt(1));
                    }
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    /**
     * Update the status of a booking (used for simple status transitions).
     */
    public void updateStatus(int bookingId, String newStatus) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt   (2, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Submit a return request — stores the return details, but waits for admin confirmation.
     */
    public void requestReturn(int bookingId, LocalDateTime actualReturnDate,
                              BigDecimal penalty, String conditionAfter) throws SQLException {
        String sql = "UPDATE bookings " +
                     "SET status='RETURN_PENDING', actual_return_date=?, penalty=?, condition_after=? " +
                     "WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp  (1, Timestamp.valueOf(actualReturnDate));
            ps.setBigDecimal (2, penalty);
            ps.setString     (3, conditionAfter);
            ps.setInt        (4, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Final admin confirmation that a pending return has been inspected and accepted.
     */
    public void confirmReturn(int bookingId, BigDecimal finalPenalty) throws SQLException {
        String sql = "UPDATE bookings SET status='RETURNED', penalty=? WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, finalPenalty);
            ps.setInt(2, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Reject a return request and restore the booking to APPROVED.
     */
    public void rejectReturn(int bookingId) throws SQLException {
        String sql = "UPDATE bookings " +
                     "SET status='APPROVED', actual_return_date=NULL, penalty=0.00, condition_after=NULL " +
                     "WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            ps.executeUpdate();
        }
    }

    /**
     * Sum requested quantities that overlap the requested booking window.
     * Pending requests are included so stock cannot be over-reserved before admin review.
     */
    public int countOverlappingOpenBookings(int itemId, LocalDateTime borrowDate, LocalDateTime returnDate)
            throws SQLException {
        String sql =
            "SELECT COALESCE(SUM(quantity), 0) FROM bookings " +
            "WHERE item_id = ? " +
            "  AND status IN ('PENDING','APPROVED','RETURN_PENDING') " +
            "  AND borrow_date <= ? " +
            "  AND return_date >= ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt      (1, itemId);
            ps.setTimestamp(2, Timestamp.valueOf(returnDate));
            ps.setTimestamp(3, Timestamp.valueOf(borrowDate));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Legacy boolean wrapper retained for older callers.
     */
    public boolean hasConflict(int itemId, LocalDateTime borrowDate, LocalDateTime returnDate) throws SQLException {
        return countOverlappingOpenBookings(itemId, borrowDate, returnDate) > 0;
    }

    /**
     * Sum approved reserved units per item.
     * A unit is considered reserved as soon as the admin approves the booking,
     * and it is released when the booking is returned or otherwise closed.
     */
    public Map<Integer, Integer> countReservedUnits() throws SQLException {
        String sql =
            "SELECT item_id, COALESCE(SUM(quantity), 0) AS units_reserved FROM bookings " +
            "WHERE status IN ('APPROVED','RETURN_PENDING') " +
            "GROUP BY item_id";

        Map<Integer, Integer> counts = new HashMap<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt("item_id"), rs.getInt("units_reserved"));
            }
        }
        return counts;
    }

    /**
     * Reserved units for a single item.
     */
    public int countReservedUnitsForItem(int itemId) throws SQLException {
        String sql =
            "SELECT COALESCE(SUM(quantity), 0) FROM bookings " +
            "WHERE item_id = ? AND status IN ('APPROVED','RETURN_PENDING')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Count of active bookings — approved or awaiting return confirmation.
     */
    public int countActive() throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM bookings " +
            "WHERE status IN ('APPROVED','RETURN_PENDING')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Count of overdue bookings (APPROVED and past return_date) — for dashboard.
     */
    public int countOverdue() throws SQLException {
        String sql =
            "SELECT COUNT(*) FROM bookings " +
            "WHERE status = 'APPROVED' AND return_date < NOW()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // --- Private helpers ---

    private void appendFilters(StringBuilder sql, List<Object> params, String keywordLike,
                               Integer bookingId, String status, boolean includeUserName) {
        if (keywordLike != null && !keywordLike.isBlank()) {
            sql.append(" AND (LOWER(i.name) LIKE ?");
            params.add(keywordLike);
            if (includeUserName) {
                sql.append(" OR LOWER(u.name) LIKE ?");
                params.add(keywordLike);
            }
            if (bookingId != null) {
                sql.append(" OR b.id = ?");
                params.add(bookingId);
            }
            sql.append(")");
        } else if (bookingId != null) {
            sql.append(" AND b.id = ?");
            params.add(bookingId);
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND b.status = ?");
            params.add(status);
        }
    }

    /** Generic query runner with positional parameters. */
    private List<Booking> query(String sql, List<Object> params) throws SQLException {
        List<Booking> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                int index = i + 1;

                if (param instanceof Integer) {
                    ps.setInt(index, (Integer) param);
                } else if (param instanceof String) {
                    ps.setString(index, (String) param);
                } else {
                    throw new SQLException("Unsupported booking query parameter type: " + param);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapRow(rs));
            }
        }
        return result;
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setId(rs.getInt("id"));
        b.setUserId(rs.getInt("user_id"));
        b.setItemId(rs.getInt("item_id"));
        b.setQuantity(rs.getInt("quantity"));

        Timestamp borrowDate = rs.getTimestamp("borrow_date");
        if (borrowDate != null) b.setBorrowDate(borrowDate.toLocalDateTime());

        Timestamp returnDate = rs.getTimestamp("return_date");
        if (returnDate != null) b.setReturnDate(returnDate.toLocalDateTime());

        Timestamp actualReturn = rs.getTimestamp("actual_return_date");
        if (actualReturn != null) b.setActualReturnDate(actualReturn.toLocalDateTime());

        b.setStatus(rs.getString("status"));
        BigDecimal penalty = rs.getBigDecimal("penalty");
        b.setPenalty(penalty != null ? penalty : BigDecimal.ZERO);
        b.setConditionBefore(rs.getString("condition_before"));
        b.setConditionAfter(rs.getString("condition_after"));

        // JOIN fields
        b.setUserName(rs.getString("user_name"));
        b.setItemName(rs.getString("item_name"));

        return b;
    }
}


package com.trackit.servlet;

import com.trackit.model.Booking;
import com.trackit.model.CategoryStockSnapshot;
import com.trackit.model.DashboardTrendPoint;
import com.trackit.model.Item;
import com.trackit.service.BookingService;
import com.trackit.service.ItemService;
import com.trackit.dao.UserDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminDashboardServlet — Renders the admin dashboard.
 *
 * Collects statistics:
 *   - Total items in catalogue
 *   - Active (APPROVED) bookings count
 *   - Overdue bookings count
 *   - Total registered users
 */
@WebServlet("/admin/dashboard")
public class AdminDashboardServlet extends HttpServlet {

    private final ItemService    itemService    = new ItemService();
    private final BookingService bookingService = new BookingService();
    private final UserDAO        userDAO        = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            List<Item> items = itemService.getAllItems();
            List<Booking> bookings = bookingService.getAllBookings();
            LocalDate today = LocalDate.now();

            int activeBookings = 0;
            int overdueBookings = 0;
            int pendingBookings = 0;
            int returnPendingBookings = 0;
            int returnedBookings = 0;
            int rejectedBookings = 0;
            int dueToday = 0;

            for (Booking booking : bookings) {
                String status = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
                switch (status) {
                    case "PENDING":
                        pendingBookings++;
                        break;
                    case "APPROVED":
                        activeBookings++;
                        if (booking.getReturnDate() != null && booking.getReturnDate().toLocalDate().isBefore(today)) {
                            overdueBookings++;
                        }
                        if (booking.getReturnDate() != null && booking.getReturnDate().toLocalDate().isEqual(today)) {
                            dueToday++;
                        }
                        break;
                    case "RETURN_PENDING":
                        activeBookings++;
                        returnPendingBookings++;
                        break;
                    case "RETURNED":
                        returnedBookings++;
                        break;
                    case "REJECTED":
                        rejectedBookings++;
                        break;
                    default:
                        break;
                }
            }

            int totalUnits = items.stream().mapToInt(Item::getQuantity).sum();
            int reservedUnits = items.stream().mapToInt(Item::getReservedUnits).sum();
            int availableUnits = items.stream().mapToInt(Item::getAvailableUnits).sum();

            req.setAttribute("totalItems", items.size());
            req.setAttribute("totalUnits", totalUnits);
            req.setAttribute("reservedUnits", reservedUnits);
            req.setAttribute("availableUnits", availableUnits);
            req.setAttribute("totalBookings", bookings.size());
            req.setAttribute("activeBookings", activeBookings);
            req.setAttribute("overdueBookings", overdueBookings);
            req.setAttribute("pendingBookings", pendingBookings);
            req.setAttribute("returnPendingBookings", returnPendingBookings);
            req.setAttribute("returnedBookings", returnedBookings);
            req.setAttribute("rejectedBookings", rejectedBookings);
            req.setAttribute("dueToday", dueToday);
            req.setAttribute("totalUsers", userDAO.countAll());
            req.setAttribute("bookingTrend", buildBorrowTrend(bookings, today));
            req.setAttribute("categoryStock", buildCategorySnapshots(items));

            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);

        } catch (SQLException e) {
            req.setAttribute("error", "Failed to load dashboard: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/admin/dashboard.jsp").forward(req, resp);
        }
    }

    private List<DashboardTrendPoint> buildBorrowTrend(List<Booking> bookings, LocalDate today) {
        Map<LocalDate, Integer> countsByDay = new LinkedHashMap<>();
        for (int offset = 6; offset >= 0; offset--) {
            countsByDay.put(today.minusDays(offset), 0);
        }

        for (Booking booking : bookings) {
            if (booking.getBorrowDate() == null) {
                continue;
            }
            LocalDate borrowDay = booking.getBorrowDate().toLocalDate();
            if (countsByDay.containsKey(borrowDay)) {
                countsByDay.put(borrowDay, countsByDay.get(borrowDay) + 1);
            }
        }

        List<DashboardTrendPoint> trend = new ArrayList<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd MMM");
        for (Map.Entry<LocalDate, Integer> entry : countsByDay.entrySet()) {
            trend.add(new DashboardTrendPoint(entry.getKey().format(labelFormatter), entry.getValue()));
        }
        return trend;
    }

    private List<CategoryStockSnapshot> buildCategorySnapshots(List<Item> items) {
        Map<String, int[]> grouped = new LinkedHashMap<>();

        for (Item item : items) {
            String category = item.getCategory() == null || item.getCategory().isBlank()
                    ? "Uncategorized"
                    : item.getCategory().trim();

            int[] totals = grouped.computeIfAbsent(category, ignored -> new int[3]);
            totals[0] += item.getQuantity();
            totals[1] += item.getReservedUnits();
            totals[2] += item.getAvailableUnits();
        }

        List<CategoryStockSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : grouped.entrySet()) {
            int[] totals = entry.getValue();
            snapshots.add(new CategoryStockSnapshot(entry.getKey(), totals[0], totals[1], totals[2]));
        }

        snapshots.sort(Comparator.comparingInt(CategoryStockSnapshot::getReservedUnits)
                .reversed()
                .thenComparing(Comparator.comparingInt(CategoryStockSnapshot::getTotalUnits).reversed())
                .thenComparing(CategoryStockSnapshot::getCategory));

        if (snapshots.size() > 6) {
            return new ArrayList<>(snapshots.subList(0, 6));
        }
        return snapshots;
    }
}


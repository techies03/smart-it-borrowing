package com.smartit.servlet;

import com.smartit.model.Booking;
import com.smartit.model.Item;
import com.smartit.model.User;
import com.smartit.service.BookingService;
import com.smartit.service.ItemService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * BookingServlet — Manages bookings for regular users.
 *
 * Routes:
 *   GET  /bookings              → list user's own bookings
 *   GET  /bookings?action=new&itemId=X → show borrow form for item X
 *   POST /bookings?action=create        → submit new booking
 *   POST /bookings?action=return&id=X   → submit return request for admin confirmation
 */
@WebServlet("/bookings")
public class BookingServlet extends HttpServlet {

    private final BookingService bookingService = new BookingService();
    private final ItemService    itemService    = new ItemService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User user   = loggedInUser(req);
        String action = req.getParameter("action");
        String keyword = req.getParameter("keyword");
        String status = req.getParameter("status");

        try {
            // Show borrow form
            if ("new".equals(action)) {
                int  itemId = Integer.parseInt(req.getParameter("itemId"));
                Item item   = itemService.getById(itemId);
                req.setAttribute("item", item);
                req.getRequestDispatcher("/WEB-INF/views/booking-form.jsp").forward(req, resp);
                return;
            }

            // List user's bookings
            List<Booking> bookings = bookingService.getBookingsByUserFiltered(user.getId(), keyword, status);
            req.setAttribute("bookings", bookings);
            req.setAttribute("keyword", keyword);
            req.setAttribute("status", status);
            req.getRequestDispatcher("/WEB-INF/views/booking-list.jsp").forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/bookings");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("keyword", keyword);
            req.setAttribute("status", status);
            req.getRequestDispatcher("/WEB-INF/views/booking-list.jsp").forward(req, resp);
        } catch (SQLException e) {
            req.setAttribute("error", "Database error: " + e.getMessage());
            req.setAttribute("keyword", keyword);
            req.setAttribute("status", status);
            req.getRequestDispatcher("/WEB-INF/views/booking-list.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        User   user   = loggedInUser(req);
        String action = req.getParameter("action");
        boolean createAction = "create".equals(action);

        try {
            if ("create".equals(action)) {
                int           itemId     = Integer.parseInt(req.getParameter("itemId"));
                int           quantity   = Integer.parseInt(req.getParameter("quantity"));
                LocalDateTime borrowDate = LocalDateTime.parse(req.getParameter("borrowDate"));
                LocalDateTime returnDate = LocalDateTime.parse(req.getParameter("returnDate"));
                String        condition  = req.getParameter("conditionBefore");

                int createdCount = bookingService.createBookings(
                        user.getId(), itemId, quantity, borrowDate, returnDate, condition);
                resp.sendRedirect(req.getContextPath() + "/bookings?success=created&createdCount=" + createdCount);

            } else if ("return".equals(action)) {
                int       bookingId     = Integer.parseInt(req.getParameter("id"));
                String    conditionAfter = req.getParameter("conditionAfter");
                BigDecimal penalty      = bookingService.requestReturn(bookingId, conditionAfter);

                resp.sendRedirect(buildBookingsRedirect(req, "success=return-requested&penalty="
                        + encode(penalty.toPlainString())));

            } else {
                resp.sendRedirect(req.getContextPath() + "/bookings");
            }

        } catch (DateTimeParseException e) {
            if (createAction) {
                req.setAttribute("error", "Please enter valid borrow and return date times.");
                reloadBookingForm(req);
                req.getRequestDispatcher("/WEB-INF/views/booking-form.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/bookings");

        } catch (NumberFormatException e) {
            if (createAction) {
                req.setAttribute("error", "Invalid booking request.");
                reloadBookingForm(req);
                req.getRequestDispatcher("/WEB-INF/views/booking-form.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/bookings");

        } catch (IllegalArgumentException e) {
            if (createAction) {
                req.setAttribute("error", e.getMessage());
                reloadBookingForm(req);
                req.getRequestDispatcher("/WEB-INF/views/booking-form.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(buildBookingsRedirect(req, "error=" + encode(e.getMessage())));

        } catch (SQLException e) {
            if (createAction) {
                req.setAttribute("error", "Database error: " + e.getMessage());
                reloadBookingForm(req);
                req.getRequestDispatcher("/WEB-INF/views/booking-form.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(buildBookingsRedirect(req, "error=" + encode("Database error: " + e.getMessage())));
        }
    }

    private User loggedInUser(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("loggedInUser");
    }

    private void reloadBookingForm(HttpServletRequest req) {
        try {
            String itemId = req.getParameter("itemId");
            if (itemId != null) {
                req.setAttribute("item", itemService.getById(Integer.parseInt(itemId)));
            }
        } catch (Exception ignored) { /* best-effort */ }
    }

    private String buildBookingsRedirect(HttpServletRequest req, String initialQuery) {
        StringBuilder redirect = new StringBuilder(req.getContextPath()).append("/bookings");
        String separator = "?";

        if (initialQuery != null && !initialQuery.isBlank()) {
            redirect.append(separator).append(initialQuery);
            separator = "&";
        }

        String keyword = req.getParameter("keyword");
        if (keyword != null && !keyword.isBlank()) {
            redirect.append(separator).append("keyword=").append(encode(keyword));
            separator = "&";
        }

        String status = req.getParameter("status");
        if (status != null && !status.isBlank()) {
            redirect.append(separator).append("status=").append(encode(status));
        }

        return redirect.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

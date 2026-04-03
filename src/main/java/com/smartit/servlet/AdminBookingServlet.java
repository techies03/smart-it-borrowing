package com.smartit.servlet;

import com.smartit.model.Booking;
import com.smartit.service.BookingService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

/**
 * AdminBookingServlet — Booking management for admins.
 *
 * URL: /admin/bookings
 * Actions (via POST ?action=):
 *   approve        → set status APPROVED
 *   reject         → set status REJECTED
 *   confirm-return → finalize a pending return
 *   reject-return  → send a return request back to APPROVED
 */
@WebServlet("/admin/bookings")
public class AdminBookingServlet extends HttpServlet {

    private final BookingService bookingService = new BookingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = req.getParameter("keyword");
        String status = req.getParameter("status");
        try {
            List<Booking> bookings = bookingService.getAllBookingsFiltered(keyword, status);
            req.setAttribute("bookings", bookings);
            req.setAttribute("keyword", keyword);
            req.setAttribute("status", status);
            req.getRequestDispatcher("/WEB-INF/views/admin/booking-management.jsp")
               .forward(req, resp);
        } catch (SQLException e) {
            req.setAttribute("error", "Failed to load bookings: " + e.getMessage());
            req.setAttribute("keyword", keyword);
            req.setAttribute("status", status);
            req.getRequestDispatcher("/WEB-INF/views/admin/booking-management.jsp")
               .forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action    = req.getParameter("action");
        String idParam   = req.getParameter("id");
        String redirectUrl = buildBookingsRedirect(req, null);

        try {
            int bookingId = Integer.parseInt(idParam);

            switch (action == null ? "" : action) {
                case "approve" -> {
                    bookingService.updateStatus(bookingId, "APPROVED");
                    resp.sendRedirect(buildBookingsRedirect(req, "success=approved"));
                }

                case "reject" -> {
                    bookingService.updateStatus(bookingId, "REJECTED");
                    resp.sendRedirect(buildBookingsRedirect(req, "success=rejected"));
                }

                case "confirm-return" -> {
                    bookingService.confirmReturn(bookingId, parseMoney(req.getParameter("damageFee")));
                    resp.sendRedirect(buildBookingsRedirect(req, "success=return-confirmed"));
                }

                case "reject-return" -> {
                    bookingService.rejectReturn(bookingId);
                    resp.sendRedirect(buildBookingsRedirect(req, "success=return-rejected"));
                }

                default -> resp.sendRedirect(redirectUrl);
            }
        } catch (NumberFormatException e) {
            resp.sendRedirect(buildBookingsRedirect(req, "error=invalid-id"));
        } catch (IllegalArgumentException e) {
            resp.sendRedirect(buildBookingsRedirect(req, "error=" + encode(e.getMessage())));
        } catch (SQLException e) {
            resp.sendRedirect(buildBookingsRedirect(req, "error=db-error"));
        }
    }

    private BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.trim());
    }

    private String buildBookingsRedirect(HttpServletRequest req, String initialQuery) {
        StringBuilder redirect = new StringBuilder(req.getContextPath()).append("/admin/bookings");
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

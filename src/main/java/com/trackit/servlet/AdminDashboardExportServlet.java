package com.trackit.servlet;

import com.trackit.model.DashboardReportData;
import com.trackit.service.DashboardExportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * AdminDashboardExportServlet — Exports admin dashboard reports as XLSX or PDF.
 */
@WebServlet("/admin/dashboard/export")
public class AdminDashboardExportServlet extends HttpServlet {

    private final DashboardExportService exportService = new DashboardExportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String reportType = normalizeReportType(req.getParameter("report"));
            String format = normalizeFormat(req.getParameter("format"));
            LocalDate fromDate = parseDate(req.getParameter("fromDate"), "Start date is required.");
            LocalDate toDate = parseDate(req.getParameter("toDate"), "End date is required.");

            DashboardReportData reportData = exportService.buildReportData(fromDate, toDate);
            String fileName = buildFileName(reportType, fromDate, toDate, format);

            resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            try (var outputStream = resp.getOutputStream()) {
                switch (format) {
                    case "xlsx" -> {
                        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                        if ("summary".equals(reportType)) {
                            exportService.writeSummaryXlsx(reportData, outputStream);
                        } else {
                            exportService.writeBookingDataXlsx(reportData, outputStream);
                        }
                    }
                    case "pdf" -> {
                        resp.setContentType("application/pdf");
                        if ("summary".equals(reportType)) {
                            exportService.writeSummaryPdf(reportData, outputStream);
                        } else {
                            exportService.writeBookingDataPdf(reportData, outputStream);
                        }
                    }
                    default -> throw new IllegalArgumentException("Unsupported export format.");
                }
                outputStream.flush();
            }
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SQLException e) {
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to prepare export data.");
        }
    }

    private String normalizeReportType(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if ("summary".equals(normalized) || "data".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Report type must be summary or data.");
    }

    private String normalizeFormat(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if ("xlsx".equals(normalized) || "pdf".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Format must be xlsx or pdf.");
    }

    private LocalDate parseDate(String value, String missingMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(missingMessage);
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date values must use the YYYY-MM-DD format.");
        }
    }

    private String buildFileName(String reportType, LocalDate fromDate, LocalDate toDate, String format) {
        String prefix = "summary".equals(reportType) ? "dashboard-summary" : "booking-data";
        return prefix + "-" + fromDate + "-to-" + toDate + "." + format;
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        if (resp.isCommitted()) {
            return;
        }

        resp.reset();
        resp.setStatus(status);
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().write(message);
    }
}


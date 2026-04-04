<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="com.trackit.model.CategoryStockSnapshot" %>
<%@ page import="com.trackit.model.DashboardTrendPoint" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard — TrackIT</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css?v=20260403-export1">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/views/partials/navbar.jsp" %>

<%
    int totalItems = getIntAttr(request, "totalItems");
    int totalUnits = getIntAttr(request, "totalUnits");
    int availableUnits = getIntAttr(request, "availableUnits");
    int reservedUnits = getIntAttr(request, "reservedUnits");
    int totalBookings = getIntAttr(request, "totalBookings");
    int activeBookings = getIntAttr(request, "activeBookings");
    int overdueBookings = getIntAttr(request, "overdueBookings");
    int pendingBookings = getIntAttr(request, "pendingBookings");
    int returnPendingBookings = getIntAttr(request, "returnPendingBookings");
    int returnedBookings = getIntAttr(request, "returnedBookings");
    int rejectedBookings = getIntAttr(request, "rejectedBookings");
    int totalUsers = getIntAttr(request, "totalUsers");
    int dueToday = getIntAttr(request, "dueToday");
    int approvedBookings = Math.max(activeBookings - returnPendingBookings, 0);

    List<DashboardTrendPoint> bookingTrend =
            (List<DashboardTrendPoint>) request.getAttribute("bookingTrend");
    List<CategoryStockSnapshot> categoryStock =
            (List<CategoryStockSnapshot>) request.getAttribute("categoryStock");
    LocalDate exportDefaultFrom = LocalDate.now().withDayOfMonth(1);
    LocalDate exportDefaultTo = LocalDate.now();

    int maxTrendValue = 0;
    if (bookingTrend != null) {
        for (DashboardTrendPoint point : bookingTrend) {
            maxTrendValue = Math.max(maxTrendValue, point.getValue());
        }
    }

    String bookingMixStyle = buildBookingMixStyle(
            totalBookings,
            pendingBookings,
            approvedBookings,
            returnPendingBookings,
            returnedBookings,
            rejectedBookings
    );
%>

<main class="container page-shell">
    <section class="hero-banner hero-banner-compact dashboard-hero">
        <div class="hero-copy">
            <span class="kicker">Dashboard</span>
            <h2>Booking overview.</h2>
        </div>

        <div class="hero-actions">
            <a href="${pageContext.request.contextPath}/items?action=new" class="btn btn-primary">Add Equipment</a>
            <a href="${pageContext.request.contextPath}/admin/bookings" class="btn btn-outline">Review Bookings</a>
            <button type="button" class="btn btn-outline" onclick="showDashboardExportModal()">Export</button>
        </div>

        <div class="hero-metrics">
            <div class="metric-chip">
                <strong><%= activeBookings %></strong>
                <span>Active bookings</span>
            </div>
            <div class="metric-chip">
                <strong><%= pendingBookings %></strong>
                <span>Waiting approval</span>
            </div>
            <div class="metric-chip">
                <strong><%= overdueBookings + returnPendingBookings %></strong>
                <span>Need attention</span>
            </div>
        </div>
    </section>

    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger">${error}</div>
    <% } %>

    <section class="stats-grid">
        <article class="stat-card stat-card-blue">
            <span class="stat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                    <rect x="3.5" y="5" width="11" height="7" rx="1.8"></rect>
                    <path d="M6.5 15.5h5"></path>
                    <path d="M8 18h2"></path>
                    <rect x="14.5" y="9" width="6" height="10" rx="1.8"></rect>
                </svg>
            </span>
            <div>
                <p class="stat-label">Equipment Entries</p>
                <h3 class="stat-value"><%= totalItems %></h3>
            </div>
        </article>
        <article class="stat-card stat-card-green">
            <span class="stat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                    <path d="M9 4h6"></path>
                    <path d="M9.5 3h5a1.5 1.5 0 0 1 1.5 1.5V6H8V4.5A1.5 1.5 0 0 1 9.5 3Z"></path>
                    <rect x="5" y="6" width="14" height="15" rx="2.2"></rect>
                    <path d="m9 14 2.2 2.2L15.5 12"></path>
                </svg>
            </span>
            <div>
                <p class="stat-label">Active Bookings</p>
                <h3 class="stat-value"><%= activeBookings %></h3>
            </div>
        </article>
        <article class="stat-card stat-card-red">
            <span class="stat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                    <circle cx="12" cy="13" r="6.5"></circle>
                    <path d="M12 9.5v4"></path>
                    <path d="M12 16.8h.01"></path>
                    <path d="M8 4.5 6.5 3"></path>
                    <path d="M16 4.5 17.5 3"></path>
                </svg>
            </span>
            <div>
                <p class="stat-label">Overdue Bookings</p>
                <h3 class="stat-value"><%= overdueBookings %></h3>
            </div>
        </article>
        <article class="stat-card stat-card-purple">
            <span class="stat-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                    <circle cx="9" cy="9" r="2.8"></circle>
                    <path d="M4.8 18a4.3 4.3 0 0 1 8.4 0"></path>
                    <circle cx="16.8" cy="8.2" r="2.2"></circle>
                    <path d="M14.2 17.2a3.5 3.5 0 0 1 5 0"></path>
                </svg>
            </span>
            <div>
                <p class="stat-label">Registered Users</p>
                <h3 class="stat-value"><%= totalUsers %></h3>
            </div>
        </article>
    </section>

    <section class="dashboard-analytics">
        <article class="surface-panel dashboard-panel">
            <div class="dashboard-panel-header">
                <div>
                    <h3>Booking Mix</h3>
                    <p>Status counts across all bookings.</p>
                </div>
                <span class="badge badge-info"><%= totalBookings %> total</span>
            </div>

            <div class="booking-mix-layout">
                <div class="booking-mix-ring" data-booking-mix-style="<%= bookingMixStyle %>">
                    <div class="booking-mix-center">
                        <strong><%= totalBookings %></strong>
                        <span>Bookings</span>
                    </div>
                </div>

                <div class="booking-legend">
                    <div class="booking-legend-row">
                        <span class="legend-swatch legend-pending"></span>
                        <span class="legend-label">Pending approvals</span>
                        <strong class="legend-value"><%= pendingBookings %> · <%= shareText(pendingBookings, totalBookings) %></strong>
                    </div>
                    <div class="booking-legend-row">
                        <span class="legend-swatch legend-approved"></span>
                        <span class="legend-label">Approved / active</span>
                        <strong class="legend-value"><%= approvedBookings %> · <%= shareText(approvedBookings, totalBookings) %></strong>
                    </div>
                    <div class="booking-legend-row">
                        <span class="legend-swatch legend-return"></span>
                        <span class="legend-label">Awaiting return check</span>
                        <strong class="legend-value"><%= returnPendingBookings %> · <%= shareText(returnPendingBookings, totalBookings) %></strong>
                    </div>
                    <div class="booking-legend-row">
                        <span class="legend-swatch legend-returned"></span>
                        <span class="legend-label">Completed returns</span>
                        <strong class="legend-value"><%= returnedBookings %> · <%= shareText(returnedBookings, totalBookings) %></strong>
                    </div>
                    <div class="booking-legend-row">
                        <span class="legend-swatch legend-rejected"></span>
                        <span class="legend-label">Rejected requests</span>
                        <strong class="legend-value"><%= rejectedBookings %> · <%= shareText(rejectedBookings, totalBookings) %></strong>
                    </div>
                </div>
            </div>
        </article>

        <article class="surface-panel dashboard-panel">
            <div class="dashboard-panel-header">
                <div>
                    <h3>7-Day Booking Trend</h3>
                    <p>Scheduled borrow starts over the last seven days.</p>
                </div>
                <span class="badge badge-secondary"><%= maxTrendValue %> peak</span>
            </div>

            <div class="trend-columns">
                <% if (bookingTrend != null && !bookingTrend.isEmpty()) {
                    for (DashboardTrendPoint point : bookingTrend) { %>
                    <div class="trend-column">
                        <span class="trend-value"><%= point.getValue() %></span>
                        <div class="trend-track">
                            <div class="trend-bar" data-height="<%= trendHeight(point.getValue(), maxTrendValue) %>"></div>
                        </div>
                        <span class="trend-label"><%= point.getLabel() %></span>
                    </div>
                <%  }
                   } %>
            </div>
        </article>
    </section>

    <section class="dashboard-secondary">
        <article class="surface-panel dashboard-panel dashboard-panel-wide">
            <div class="dashboard-panel-header">
                <div>
                    <h3>Category Stock</h3>
                    <p>Available and reserved units by category.</p>
                </div>
                <span class="badge badge-success"><%= availableUnits %> units available</span>
            </div>

            <div class="category-stock-list">
                <% if (categoryStock != null && !categoryStock.isEmpty()) {
                    for (CategoryStockSnapshot snapshot : categoryStock) { %>
                    <div class="category-stock-row">
                        <div class="category-stock-top">
                            <div>
                                <strong class="category-stock-name"><%= snapshot.getCategory() %></strong>
                                <span class="category-stock-meta"><%= snapshot.getAvailableUnits() %> available · <%= snapshot.getReservedUnits() %> reserved</span>
                            </div>
                            <strong class="category-stock-total"><%= snapshot.getTotalUnits() %> units</strong>
                        </div>
                        <div class="category-stock-bar">
                            <span class="category-stock-available" data-width="<%= widthPercent(snapshot.getAvailableUnits(), snapshot.getTotalUnits()) %>"></span>
                            <span class="category-stock-reserved" data-width="<%= widthPercent(snapshot.getReservedUnits(), snapshot.getTotalUnits()) %>"></span>
                        </div>
                    </div>
                <%  }
                   } else { %>
                    <p class="section-note">No category data yet.</p>
                <% } %>
            </div>
        </article>

        <article class="surface-panel dashboard-panel">
            <div class="dashboard-panel-header">
                <div>
                    <h3>Today</h3>
                    <p>Items that need attention.</p>
                </div>
            </div>

            <div class="focus-grid">
                <div class="focus-card">
                    <span class="focus-label">Due Today</span>
                    <strong><%= dueToday %></strong>
                    <p>Approved bookings due back today.</p>
                </div>
                <div class="focus-card">
                    <span class="focus-label">Return Checks</span>
                    <strong><%= returnPendingBookings %></strong>
                    <p>Waiting for return confirmation.</p>
                </div>
                <div class="focus-card">
                    <span class="focus-label">Reserved Units</span>
                    <strong><%= reservedUnits %></strong>
                    <p>Locked by approved bookings.</p>
                </div>
                <div class="focus-card">
                    <span class="focus-label">Available Units</span>
                    <strong><%= availableUnits %></strong>
                    <p>Currently available to book.</p>
                </div>
            </div>
        </article>
    </section>

    <section class="action-grid">
        <article class="action-card">
            <span class="icon-badge">＋</span>
            <div>
                <h3 class="action-card-title">Add Equipment</h3>
                <p class="action-card-copy">Create a new catalogue item.</p>
            </div>
            <a href="${pageContext.request.contextPath}/items?action=new" class="btn btn-primary btn-sm">Create Item</a>
        </article>

        <article class="action-card">
            <span class="icon-badge">↺</span>
            <div>
                <h3 class="action-card-title">Manage Bookings</h3>
                <p class="action-card-copy">Review requests, approvals, and returns.</p>
            </div>
            <a href="${pageContext.request.contextPath}/admin/bookings" class="btn btn-outline btn-sm">Open Bookings</a>
        </article>

        <article class="action-card">
            <span class="icon-badge">⌁</span>
            <div>
                <h3 class="action-card-title">View Catalogue</h3>
                <p class="action-card-copy">Open the borrower-facing catalogue.</p>
            </div>
            <a href="${pageContext.request.contextPath}/items" class="btn btn-outline btn-sm">View Catalogue</a>
        </article>
    </section>
</main>

<div id="dashboard-export-modal" class="modal-overlay" style="display:none">
    <div class="modal-box modal-box-wide">
        <h3>Export Dashboard Report</h3>
        <p class="form-hint">
            Choose what to export, the file format, and the borrow-date range to include in the report.
        </p>

        <form id="dashboard-export-form"
              action="${pageContext.request.contextPath}/admin/dashboard/export"
              method="get">
            <div class="field-grid">
                <div class="form-group">
                    <label for="dashboard-export-report">Report Type</label>
                    <div class="select-shell">
                        <select id="dashboard-export-report" name="report" class="form-control" required>
                            <option value="summary">Dashboard Summary</option>
                            <option value="data">Booking Data</option>
                        </select>
                    </div>
                </div>

                <div class="form-group">
                    <label for="dashboard-export-format">Format</label>
                    <div class="select-shell">
                        <select id="dashboard-export-format" name="format" class="form-control" required>
                            <option value="xlsx">Excel (.xlsx)</option>
                            <option value="pdf">PDF (.pdf)</option>
                        </select>
                    </div>
                </div>
            </div>

            <div class="field-grid">
                <div class="form-group">
                    <label for="dashboard-export-from-date">From Date</label>
                    <input
                            type="date"
                            id="dashboard-export-from-date"
                            name="fromDate"
                            class="form-control"
                            value="<%= exportDefaultFrom %>"
                            required>
                </div>

                <div class="form-group">
                    <label for="dashboard-export-to-date">To Date</label>
                    <input
                            type="date"
                            id="dashboard-export-to-date"
                            name="toDate"
                            class="form-control"
                            value="<%= exportDefaultTo %>"
                            required>
                </div>
            </div>

            <div class="dashboard-export-note" id="dashboard-export-note">
                Dashboard Summary includes the current equipment snapshot plus booking metrics and trends for borrow dates inside the selected range.
            </div>

            <div class="form-actions">
                <button type="submit" class="btn btn-primary" id="dashboard-export-submit">Download Export</button>
                <button type="button" class="btn btn-outline" onclick="hideDashboardExportModal()">Cancel</button>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/static/js/app.js?v=20260403-export1"></script>
<script>
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-booking-mix-style]').forEach(function (element) {
            element.style.background = element.getAttribute('data-booking-mix-style');
        });

        document.querySelectorAll('.trend-bar[data-height]').forEach(function (element) {
            element.style.height = element.getAttribute('data-height') + 'px';
        });

        document.querySelectorAll('[data-width]').forEach(function (element) {
            element.style.width = element.getAttribute('data-width');
        });
    });
</script>
</body>
</html>
<%!
    private int getIntAttr(jakarta.servlet.http.HttpServletRequest request, String name) {
        Object value = request.getAttribute(name);
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private int percent(int value, int total) {
        return total > 0 ? Math.round((value * 100f) / total) : 0;
    }

    private String shareText(int value, int total) {
        return percent(value, total) + "%";
    }

    private int trendHeight(int value, int maxValue) {
        if (maxValue <= 0 || value <= 0) {
            return 16;
        }
        return Math.max(16, Math.round((value * 132f) / maxValue));
    }

    private String widthPercent(int value, int total) {
        if (value <= 0 || total <= 0) {
            return "0%";
        }
        return percent(value, total) + "%";
    }

    private String buildBookingMixStyle(int total, int pending, int approved,
                                        int returnPending, int returned, int rejected) {
        if (total <= 0) {
            return "conic-gradient(rgba(123,135,127,.18) 0 100%)";
        }

        double cursor = 0;
        StringBuilder gradient = new StringBuilder("conic-gradient(");
        cursor = appendSegment(gradient, "var(--warning)", pending, total, cursor);
        cursor = appendSegment(gradient, "var(--success)", approved, total, cursor);
        cursor = appendSegment(gradient, "var(--info)", returnPending, total, cursor);
        cursor = appendSegment(gradient, "rgba(123,135,127,.8)", returned, total, cursor);
        appendSegment(gradient, "var(--danger)", rejected, total, cursor);
        gradient.append(")");
        return gradient.toString();
    }

    private double appendSegment(StringBuilder gradient, String color, int value, int total, double start) {
        if (value <= 0 || total <= 0) {
            return start;
        }

        double end = start + ((value * 100d) / total);
        if (gradient.charAt(gradient.length() - 1) != '(') {
            gradient.append(", ");
        }
        gradient.append(color)
                .append(' ')
                .append(start)
                .append("% ")
                .append(end)
                .append('%');
        return end;
    }
%>


<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.smartit.model.Booking" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Bookings — Smart IT Borrowing</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/views/partials/navbar.jsp" %>

<%
    List<Booking> bookings = (List<Booking>) request.getAttribute("bookings");
    String keywordValue = request.getAttribute("keyword") != null ? request.getAttribute("keyword").toString() : "";
    String statusValue = request.getAttribute("status") != null ? request.getAttribute("status").toString() : "";
    boolean hasFilters = (keywordValue != null && !keywordValue.isBlank())
            || (statusValue != null && !statusValue.isBlank());
    int pendingCount = 0;
    int approvedCount = 0;
    int returnPendingCount = 0;
    int returnedCount = 0;
    if (bookings != null) {
        for (Booking b : bookings) {
            if ("PENDING".equalsIgnoreCase(b.getStatus())) pendingCount++;
            if ("APPROVED".equalsIgnoreCase(b.getStatus())) approvedCount++;
            if ("RETURN_PENDING".equalsIgnoreCase(b.getStatus())) returnPendingCount++;
            if ("RETURNED".equalsIgnoreCase(b.getStatus())) returnedCount++;
        }
    }
%>

<main class="container page-shell">
    <section class="hero-banner hero-banner-compact">
        <div class="hero-copy">
            <span class="kicker">My Bookings</span>
            <h2>Your bookings.</h2>
        </div>

        <div class="hero-actions">
            <a href="${pageContext.request.contextPath}/items" class="btn btn-primary">Borrow More</a>
        </div>

        <div class="hero-metrics">
            <div class="metric-chip">
                <strong><%= bookings != null ? bookings.size() : 0 %></strong>
                <span>Total bookings</span>
            </div>
            <div class="metric-chip">
                <strong><%= approvedCount %></strong>
                <span>Approved</span>
            </div>
            <div class="metric-chip">
                <strong><%= returnPendingCount %></strong>
                <span>Return pending</span>
            </div>
        </div>
    </section>

    <% String success = request.getParameter("success"); %>
    <% if ("created".equals(success)) { %>
        <%
            String createdCount = request.getParameter("createdCount");
            boolean multipleCreated = createdCount != null && !"1".equals(createdCount);
        %>
        <div class="alert alert-success">
            <%= multipleCreated
                    ? safeText(createdCount) + " booking entries were created and are now waiting for admin approval."
                    : "Borrow request submitted. It is now waiting for admin approval." %>
        </div>
    <% } else if ("return-requested".equals(success)) {
        String penalty = request.getParameter("penalty"); %>
        <div class="alert alert-info">
            Return request submitted. It is now waiting for admin confirmation.
            <% if (penalty != null && !penalty.equals("0.00")) { %>
                <strong> Estimated penalty: RM<%= penalty %></strong>
            <% } %>
        </div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger">${error}</div>
    <% } else if (request.getParameter("error") != null) { %>
        <div class="alert alert-danger"><%= safeText(request.getParameter("error")) %></div>
    <% } %>

    <section class="surface-panel toolbar-panel">
        <div class="table-title-group">
            <h3 class="section-title">Filter My Bookings</h3>
            <p class="section-note">Search by item name or booking ID, or filter by status.</p>
        </div>

        <form method="get" action="${pageContext.request.contextPath}/bookings" class="toolbar-grid">
            <div class="form-group">
                <label for="keyword">Search</label>
                <input type="text" id="keyword" name="keyword" class="form-control"
                       placeholder="Laptop, projector, BK-0004..." value="<%= keywordValue %>">
            </div>

            <div class="form-group">
                <label for="status">Status</label>
                <div class="select-shell">
                    <select id="status" name="status" class="form-control">
                        <option value="">All Statuses</option>
                        <option value="PENDING" <%= "PENDING".equalsIgnoreCase(statusValue) ? "selected" : "" %>>Pending</option>
                        <option value="APPROVED" <%= "APPROVED".equalsIgnoreCase(statusValue) ? "selected" : "" %>>Approved</option>
                        <option value="RETURN_PENDING" <%= "RETURN_PENDING".equalsIgnoreCase(statusValue) ? "selected" : "" %>>Return Pending</option>
                        <option value="RETURNED" <%= "RETURNED".equalsIgnoreCase(statusValue) ? "selected" : "" %>>Returned</option>
                        <option value="REJECTED" <%= "REJECTED".equalsIgnoreCase(statusValue) ? "selected" : "" %>>Rejected</option>
                    </select>
                </div>
            </div>

            <div class="toolbar-actions">
                <button class="btn btn-primary" type="submit">Search</button>
                <a href="${pageContext.request.contextPath}/bookings" class="btn btn-outline">Clear</a>
            </div>
        </form>
    </section>

    <% if (bookings == null || bookings.isEmpty()) { %>
        <section class="surface-panel empty-state">
            <span class="empty-icon">⌁</span>
            <p><%= hasFilters
                    ? "No bookings matched this filter. Try a broader search or clear the current status."
                    : "No bookings yet. Start from the catalogue and send your first request." %></p>
        </section>
    <% } else { %>
        <section class="surface-panel toolbar-panel">
            <div class="table-title-group">
                <h3 class="section-title">Status Summary</h3>
                <p class="section-note"><%= pendingCount %> pending, <%= approvedCount %> approved, <%= returnPendingCount %> awaiting return review, <%= returnedCount %> returned.</p>
            </div>
        </section>

        <section class="surface-panel table-panel">
            <div class="table-header">
                <div class="table-title-group">
                    <h3 class="section-title">Bookings</h3>
                    <p class="section-note">All booking timestamps are shown here.</p>
                </div>
            </div>

            <div class="table-wrapper">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Booking ID</th>
                            <th>Item</th>
                            <th>Qty</th>
                            <th>Borrow</th>
                            <th>Expected Return</th>
                            <th>Actual Return</th>
                            <th>Status</th>
                            <th>Penalty</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% for (Booking b : bookings) { %>
                        <tr>
                            <td><strong><%= b.getReferenceCode() %></strong></td>
                            <td><strong><%= b.getItemName() != null ? b.getItemName() : "#" + b.getItemId() %></strong></td>
                            <td><%= b.getQuantity() %></td>
                            <td><%= formatDateTime(b.getBorrowDate()) %></td>
                            <td><%= formatDateTime(b.getReturnDate()) %></td>
                            <td><%= formatDateTime(b.getActualReturnDate()) %></td>
                            <td>
                                <span class="badge badge-<%= statusClass(b.getStatus()) %>"><%= b.getStatus() %></span>
                            </td>
                            <td><%= b.getPenalty() != null && b.getPenalty().doubleValue() > 0 ? "RM" + b.getPenalty().toPlainString() : "—" %></td>
                            <td>
                                <div class="table-actions">
                                    <% if ("APPROVED".equals(b.getStatus()) && b.getActualReturnDate() == null) { %>
                                        <button type="button" class="btn btn-outline btn-sm"
                                                data-booking-id="<%= b.getId() %>"
                                                onclick="showReturnModal(this.dataset.bookingId)">Return Item</button>
                                    <% } else if ("RETURN_PENDING".equals(b.getStatus())) { %>
                                        <span class="text-muted">Awaiting admin check</span>
                                    <% } else { %>
                                        <span class="text-muted">No action</span>
                                    <% } %>
                                </div>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </section>
    <% } %>
</main>

<div id="return-modal" class="modal-overlay" style="display:none">
    <div class="modal-box">
        <h3>Return Equipment</h3>
        <form method="post" action="${pageContext.request.contextPath}/bookings">
            <input type="hidden" name="action" value="return">
            <input type="hidden" name="id" id="returnBookingId">
            <input type="hidden" name="keyword" value="<%= keywordValue %>">
            <input type="hidden" name="status" value="<%= statusValue %>">
            <div class="form-group">
                <label for="conditionAfter">Condition on Return</label>
                <div class="select-shell">
                    <select id="conditionAfter" name="conditionAfter" class="form-control">
                        <option value="GOOD">Good</option>
                        <option value="DAMAGED">Damaged</option>
                    </select>
                </div>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">Confirm Return</button>
                <button type="button" class="btn btn-outline" onclick="hideReturnModal()">Cancel</button>
            </div>
        </form>
    </div>
</div>

<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>
<%!
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMATTER) : "—";
    }

    private String statusClass(String status) {
        if (status == null) return "secondary";
        switch (status) {
            case "APPROVED": return "success";
            case "PENDING": return "warning";
            case "RETURN_PENDING": return "info";
            case "REJECTED": return "danger";
            case "RETURNED": return "secondary";
            default: return "secondary";
        }
    }

    private String safeText(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
%>

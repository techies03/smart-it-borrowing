<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalDateTime" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.trackit.model.Booking" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Manage Bookings — TrackIT</title>
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
            <span class="kicker">Booking Management</span>
            <h2>Review bookings.</h2>
        </div>

        <div class="hero-actions">
            <a href="${pageContext.request.contextPath}/admin/dashboard" class="btn btn-outline">Dashboard</a>
            <a href="${pageContext.request.contextPath}/items" class="btn btn-primary">Catalogue</a>
        </div>

        <div class="hero-metrics">
            <div class="metric-chip">
                <strong><%= bookings != null ? bookings.size() : 0 %></strong>
                <span>Total requests</span>
            </div>
            <div class="metric-chip">
                <strong><%= pendingCount %></strong>
                <span>Waiting approval</span>
            </div>
            <div class="metric-chip">
                <strong><%= returnPendingCount %></strong>
                <span>Waiting return check</span>
            </div>
        </div>
    </section>

    <% String success = request.getParameter("success"); %>
    <% if ("approved".equals(success)) { %><div class="alert alert-success">Booking approved.</div><% } %>
    <% if ("rejected".equals(success)) { %><div class="alert alert-warning">Booking rejected.</div><% } %>
    <% if ("return-confirmed".equals(success)) { %><div class="alert alert-success">Return confirmed and booking closed.</div><% } %>
    <% if ("return-rejected".equals(success)) { %><div class="alert alert-warning">Return request rejected and booking restored to APPROVED.</div><% } %>
    <% String err = request.getParameter("error"); %>
    <% if (err != null) { %><div class="alert alert-danger">Error: <%= err.replaceAll("[<>&\"']", "") %></div><% } %>
    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger">${error}</div>
    <% } %>

    <% if (bookings != null && !bookings.isEmpty()) { %>
        <section class="surface-panel toolbar-panel">
            <div class="table-title-group">
                <h3 class="section-title">Status Summary</h3>
                <p class="section-note"><%= pendingCount %> pending approval, <%= returnPendingCount %> waiting return check, <%= approvedCount %> active, <%= returnedCount %> completed.</p>
            </div>
        </section>
    <% } %>

    <section class="surface-panel toolbar-panel">
        <div class="table-title-group">
            <h3 class="section-title">Filter Bookings</h3>
            <p class="section-note">Search by borrower, item, or booking ID, or filter by status.</p>
        </div>

        <form method="get" action="${pageContext.request.contextPath}/admin/bookings" class="toolbar-grid">
            <div class="form-group">
                <label for="keyword">Search</label>
                <input type="text" id="keyword" name="keyword" class="form-control"
                       placeholder="Borrower, item, BK-0004..." value="<%= keywordValue %>">
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
                <a href="${pageContext.request.contextPath}/admin/bookings" class="btn btn-outline">Clear</a>
            </div>
        </form>
    </section>

    <% if (bookings == null || bookings.isEmpty()) { %>
        <section class="surface-panel empty-state">
            <span class="empty-icon">⌁</span>
            <p><%= hasFilters
                    ? "No bookings matched this filter. Try a broader search or clear the current status."
                    : "No bookings are in the system yet. Once requests arrive, they will appear here for review." %></p>
        </section>
    <% } else { %>

        <section class="surface-panel table-panel">
            <div class="table-header">
                <div class="table-title-group">
                    <h3 class="section-title">Bookings</h3>
                    <p class="section-note">Borrower, item, timing, and penalty details.</p>
                </div>
            </div>

            <div class="table-wrapper">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Booking ID</th>
                            <th>User</th>
                            <th>Item</th>
                            <th>Qty</th>
                            <th>Borrow</th>
                            <th>Expected Return</th>
                            <th>Actual Return</th>
                            <th>Return Condition</th>
                            <th>Status</th>
                            <th>Penalty</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% for (Booking b : bookings) { %>
                        <%
                            boolean damageFeeRequired =
                                    "GOOD".equalsIgnoreCase(b.getConditionBefore())
                                 && "DAMAGED".equalsIgnoreCase(b.getConditionAfter());
                            String lateFeeDisplay = "RM" + (b.getPenalty() != null ? b.getPenalty().toPlainString() : "0.00");
                            String bookingRef = b.getReferenceCode();
                        %>
                        <tr>
                            <td><strong><%= bookingRef %></strong></td>
                            <td><%= b.getUserName() != null ? b.getUserName() : "#" + b.getUserId() %></td>
                            <td><%= b.getItemName() != null ? b.getItemName() : "#" + b.getItemId() %></td>
                            <td><%= b.getQuantity() %></td>
                            <td><%= formatDateTime(b.getBorrowDate()) %></td>
                            <td><%= formatDateTime(b.getReturnDate()) %></td>
                            <td><%= formatDateTime(b.getActualReturnDate()) %></td>
                            <td><%= b.getConditionAfter() != null ? b.getConditionAfter() : "—" %></td>
                            <td><span class="badge badge-<%= statusClass(b.getStatus()) %>"><%= b.getStatus() %></span></td>
                            <td><%= b.getPenalty() != null && b.getPenalty().doubleValue() > 0 ? "RM" + b.getPenalty().toPlainString() : "—" %></td>
                            <td>
                                <div class="table-actions">
                                    <% if ("PENDING".equals(b.getStatus())) { %>
                                        <form method="post" action="${pageContext.request.contextPath}/admin/bookings" class="inline-form"
                                              data-confirm-title="Approve booking?"
                                              data-confirm-message="This will approve <%= bookingRef %> for <%= b.getUserName() != null ? b.getUserName() : "this user" %> and reserve <%= b.getQuantity() %> unit(s)."
                                              data-confirm-button="Approve Booking"
                                              data-confirm-tone="success">
                                            <input type="hidden" name="action" value="approve">
                                            <input type="hidden" name="id" value="<%= b.getId() %>">
                                            <input type="hidden" name="keyword" value="<%= keywordValue %>">
                                            <input type="hidden" name="status" value="<%= statusValue %>">
                                            <button type="submit" class="btn btn-success btn-sm">Approve</button>
                                        </form>
                                        <form method="post" action="${pageContext.request.contextPath}/admin/bookings" class="inline-form"
                                              data-confirm-title="Reject booking?"
                                              data-confirm-message="This will reject <%= bookingRef %> and close this request."
                                              data-confirm-button="Reject Booking"
                                              data-confirm-tone="danger">
                                            <input type="hidden" name="action" value="reject">
                                            <input type="hidden" name="id" value="<%= b.getId() %>">
                                            <input type="hidden" name="keyword" value="<%= keywordValue %>">
                                            <input type="hidden" name="status" value="<%= statusValue %>">
                                            <button type="submit" class="btn btn-danger btn-sm">Reject</button>
                                        </form>
                                    <% } else if ("RETURN_PENDING".equals(b.getStatus())) { %>
                                        <form method="post" action="${pageContext.request.contextPath}/admin/bookings" class="inline-form"
                                              data-confirm-title="Confirm returned item?"
                                              data-confirm-message="This will close <%= bookingRef %> as returned after your physical inspection for <%= b.getQuantity() %> unit(s)."
                                              data-damage-fee-required="<%= damageFeeRequired %>"
                                              data-damage-fee-default="<%= damageFeeRequired ? "0.00" : "0.00" %>"
                                              data-damage-fee-note="<%= damageFeeRequired ? "Late fee currently " + lateFeeDisplay + ". Add the extra damage charge because the item was borrowed as GOOD and returned DAMAGED." : "Late fee currently " + lateFeeDisplay + ". No damage fee is required for this return." %>"
                                              data-confirm-button="Confirm Return"
                                              data-confirm-tone="success">
                                            <input type="hidden" name="action" value="confirm-return">
                                            <input type="hidden" name="id" value="<%= b.getId() %>">
                                            <input type="hidden" name="damageFee" value="0.00">
                                            <input type="hidden" name="keyword" value="<%= keywordValue %>">
                                            <input type="hidden" name="status" value="<%= statusValue %>">
                                            <button type="submit" class="btn btn-success btn-sm">Confirm Return</button>
                                        </form>
                                        <form method="post" action="${pageContext.request.contextPath}/admin/bookings" class="inline-form"
                                              data-confirm-title="Reject return request?"
                                              data-confirm-message="This will send <%= bookingRef %> back to APPROVED so the borrower can resubmit after correction."
                                              data-confirm-button="Reject Return"
                                              data-confirm-tone="danger">
                                            <input type="hidden" name="action" value="reject-return">
                                            <input type="hidden" name="id" value="<%= b.getId() %>">
                                            <input type="hidden" name="keyword" value="<%= keywordValue %>">
                                            <input type="hidden" name="status" value="<%= statusValue %>">
                                            <button type="submit" class="btn btn-danger btn-sm">Reject Return</button>
                                        </form>
                                    <% } else { %>
                                        <span class="text-muted">Handled</span>
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
%>


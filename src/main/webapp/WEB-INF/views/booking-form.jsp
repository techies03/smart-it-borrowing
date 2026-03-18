<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartit.model.Item" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Borrow Item — Smart IT Borrowing</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/views/partials/navbar.jsp" %>

<main class="container container-narrow page-shell">
    <%
        Item item = (Item) request.getAttribute("item");
        String quantityValue = request.getParameter("quantity");
        String borrowValue = request.getParameter("borrowDate");
        String returnValue = request.getParameter("returnDate");
        String selectedCondition = request.getParameter("conditionBefore");
        if (selectedCondition == null || selectedCondition.isBlank()) {
            selectedCondition = "GOOD";
        }
    %>

    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger">${error}</div>
    <% } %>

    <% if (item != null) { %>
        <div class="form-shell">
            <section class="surface-panel form-aside">
                <div>
                    <span class="kicker">Borrow Request</span>
                    <h2>Create a booking for this item.</h2>
                    <p>Available now: <%= item.getAvailableUnits() %> of <%= item.getQuantity() %> unit(s).</p>
                </div>

                <div class="item-summary-card">
                    <% if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) { %>
                        <img src="${pageContext.request.contextPath}<%= item.getImageUrl() %>"
                             alt="<%= item.getName() %>"
                             class="item-thumbnail-lg">
                    <% } else { %>
                        <span class="item-icon-lg">⌘</span>
                    <% } %>
                    <div>
                        <h3><%= item.getName() %></h3>
                        <p><%= item.getCategory() %> · Condition: <%= item.getItemCondition() %></p>
                    </div>
                </div>

                <div class="helper-list">
                    <div class="helper-item">
                        <span>Category</span>
                        <strong><%= item.getCategory() %></strong>
                    </div>
                    <div class="helper-item">
                        <span>Available Stock</span>
                        <strong><%= item.getAvailableUnits() %> of <%= item.getQuantity() %></strong>
                    </div>
                    <div class="helper-item">
                        <span>Max Per Request</span>
                        <strong>Up to <%= item.getQuantity() %> unit(s)</strong>
                    </div>
                    <div class="helper-item">
                        <span>Approved Reservations</span>
                        <strong><%= item.getReservedUnits() %></strong>
                    </div>
                    <div class="helper-item">
                        <span>Condition</span>
                        <strong><%= item.getItemCondition() %></strong>
                    </div>
                </div>

                <div class="notice-card">
                    Requesting more than 1 unit creates separate booking IDs for each unit.
                </div>
            </section>

            <section class="surface-panel form-panel">
                <div class="form-panel-header">
                    <h3>Create Booking</h3>
                    <p class="section-note">Choose quantity, dates, and observed condition.</p>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/bookings">
                    <input type="hidden" name="action" value="create">
                    <input type="hidden" name="itemId" value="<%= item.getId() %>">

                    <div class="field-grid">
                        <div class="form-group">
                            <label for="borrowDate">Borrow Date &amp; Time</label>
                            <input type="datetime-local" id="borrowDate" name="borrowDate" class="form-control"
                                   value="<%= borrowValue != null ? borrowValue : "" %>" required>
                        </div>

                        <div class="form-group">
                            <label for="returnDate">Expected Return</label>
                            <input type="datetime-local" id="returnDate" name="returnDate" class="form-control"
                                   value="<%= returnValue != null ? returnValue : "" %>" required>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="quantity">Quantity To Borrow</label>
                        <input type="number" id="quantity" name="quantity" class="form-control"
                               value="<%= quantityValue != null && !quantityValue.isBlank() ? quantityValue : "1" %>"
                               min="1" max="<%= Math.max(item.getQuantity(), 1) %>" required>
                        <p class="form-hint">Submitting more than 1 will split the request into separate one-unit booking entries with different booking IDs.</p>
                    </div>

                    <div class="form-group">
                        <label for="conditionBefore">Observed Condition</label>
                        <div class="select-shell">
                            <select id="conditionBefore" name="conditionBefore" class="form-control">
                                <option value="GOOD" <%= "GOOD".equals(selectedCondition) ? "selected" : "" %>>Good</option>
                                <option value="DAMAGED" <%= "DAMAGED".equals(selectedCondition) ? "selected" : "" %>>Damaged</option>
                            </select>
                        </div>
                    </div>

                    <div class="form-actions">
                        <button type="submit" class="btn btn-primary">Submit Request</button>
                        <a href="${pageContext.request.contextPath}/items" class="btn btn-outline">Back To Catalogue</a>
                    </div>
                </form>
            </section>
        </div>
    <% } else { %>
        <section class="surface-panel empty-state">
            <span class="empty-icon">⌂</span>
            <p>This item could not be loaded. Head back to the catalogue and start again.</p>
        </section>
    <% } %>
</main>

<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
<script>
function toDateTimeLocal(date) {
    const pad = (value) => String(value).padStart(2, '0');
    return date.getFullYear()
        + '-' + pad(date.getMonth() + 1)
        + '-' + pad(date.getDate())
        + 'T' + pad(date.getHours())
        + ':' + pad(date.getMinutes());
}

document.addEventListener('DOMContentLoaded', function () {
    const borrowInput = document.getElementById('borrowDate');
    const returnInput = document.getElementById('returnDate');
    if (!borrowInput || !returnInput) return;

    const minValue = toDateTimeLocal(new Date());
    borrowInput.min = minValue;
    returnInput.min = borrowInput.value || minValue;

    borrowInput.addEventListener('change', function () {
        returnInput.min = this.value || minValue;
        if (returnInput.value && returnInput.value < returnInput.min) {
            returnInput.value = returnInput.min;
        }
    });
});
</script>
</body>
</html>

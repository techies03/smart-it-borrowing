<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.trackit.model.Item" %>
<%@ page import="com.trackit.model.User" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IT Equipment — TrackIT</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/views/partials/navbar.jsp" %>

<%
    User u = (User) session.getAttribute("loggedInUser");
    List<Item> items = (List<Item>) request.getAttribute("items");
    List<String> cats = (List<String>) request.getAttribute("categories");
    int totalItems = items != null ? items.size() : 0;
    int availableItems = 0;
    if (items != null) {
        for (Item item : items) {
            if (item.hasAvailableUnits()) {
                availableItems++;
            }
        }
    }
    int categoryCount = cats != null ? cats.size() : 0;
%>

<main class="container page-shell">
    <section class="hero-banner hero-banner-compact">
        <div class="hero-copy">
            <span class="kicker">Catalogue</span>
            <h2>Browse equipment.</h2>
        </div>

        <div class="hero-actions">
            <% if (u != null && u.isAdmin()) { %>
                <a href="${pageContext.request.contextPath}/items?action=new" class="btn btn-primary">Add Item</a>
            <% } %>
            <a href="${pageContext.request.contextPath}/bookings" class="btn btn-outline">My Bookings</a>
        </div>

        <div class="hero-metrics">
            <div class="metric-chip">
                <strong><%= totalItems %></strong>
                <span>Items</span>
            </div>
            <div class="metric-chip">
                <strong><%= availableItems %></strong>
                <span>Available items</span>
            </div>
            <div class="metric-chip">
                <strong><%= categoryCount %></strong>
                <span>Categories</span>
            </div>
        </div>
    </section>

    <% String success = request.getParameter("success"); %>
    <% if (success != null) { %>
        <div class="alert alert-success">
            <% if ("created".equals(success)) { %>Item added successfully.<% } %>
            <% if ("updated".equals(success)) { %>Item updated successfully.<% } %>
            <% if ("deleted".equals(success)) { %>Item deleted.<% } %>
        </div>
    <% } %>
    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger">${error}</div>
    <% } %>

    <section class="surface-panel toolbar-panel">
        <div class="table-title-group">
            <h3 class="section-title">Filter Catalogue</h3>
            <p class="section-note">Search by name or filter by category.</p>
        </div>

        <form method="get" action="${pageContext.request.contextPath}/items" class="toolbar-grid">
            <div class="form-group">
                <label for="keyword">Search</label>
                <input type="text" id="keyword" name="keyword" class="form-control"
                       placeholder="Laptop, webcam, adapter..." value="${keyword}">
            </div>

            <div class="form-group">
                <label for="category">Category</label>
                <div class="select-shell">
                    <select id="category" name="category" class="form-control">
                        <option value="">All Categories</option>
                        <%
                            if (cats != null) {
                                for (String cat : cats) {
                        %>
                            <option value="<%= cat %>" <%= cat.equals(request.getAttribute("category")) ? "selected" : "" %>><%= cat %></option>
                        <%      }
                            }
                        %>
                    </select>
                </div>
            </div>

            <div class="toolbar-actions">
                <button class="btn btn-primary" type="submit">Search</button>
                <a href="${pageContext.request.contextPath}/items" class="btn btn-outline">Clear</a>
            </div>
        </form>
    </section>

    <% if (items == null || items.isEmpty()) { %>
        <section class="surface-panel empty-state">
            <span class="empty-icon">⌂</span>
            <p>No equipment matched this filter. Try a broader search or clear the category.</p>
        </section>
    <% } else { %>
        <section class="item-grid">
            <% for (Item item : items) { %>
                <article class="item-card">
                    <div class="item-card-media-wrap">
                        <span class="pill-tag"><%= item.getAvailableUnits() %> / <%= item.getQuantity() %> available</span>
                        <% if (item.getImageUrl() != null && !item.getImageUrl().isBlank()) { %>
                            <div class="item-card-media">
                                <img src="${pageContext.request.contextPath}<%= item.getImageUrl() %>"
                                     alt="<%= item.getName() %>"
                                     class="item-card-image">
                            </div>
                        <% } else { %>
                            <span class="item-icon">⌘</span>
                        <% } %>
                    </div>

                    <div class="item-card-body">
                        <div class="item-card-top">
                            <span class="item-category-pill"><%= item.getCategory() %></span>
                            <% if (!item.isRequestable()) { %>
                                <span class="badge badge-warning">Unavailable</span>
                            <% } else if (item.hasAvailableUnits()) { %>
                                <span class="badge badge-success"><%= item.getAvailableUnits() %> Available</span>
                            <% } else { %>
                                <span class="badge badge-info">Fully Reserved</span>
                            <% } %>
                        </div>

                        <div>
                            <h3><%= item.getName() %></h3>
                            <p class="item-description">Condition: <%= item.getItemCondition() %></p>
                        </div>

                        <div class="meta-list">
                            <div>
                                <span class="meta-label">Available Stock</span>
                                <span class="meta-value"><%= item.getAvailableUnits() %> of <%= item.getQuantity() %></span>
                            </div>
                            <div>
                                <span class="meta-label">Approved Reservations</span>
                                <span class="meta-value"><%= item.getReservedUnits() %></span>
                            </div>
                            <div class="meta-wide">
                                <span class="meta-label">Inventory State</span>
                                <span class="meta-value"><%= item.getStatus() %> · <%= item.getItemCondition() %></span>
                            </div>
                        </div>
                    </div>

                    <div class="item-card-footer">
                        <div class="item-card-primary-action">
                            <% if (item.isRequestable()) { %>
                                <a href="${pageContext.request.contextPath}/bookings?action=new&itemId=<%= item.getId() %>"
                                   class="btn btn-primary btn-sm"><%= item.hasAvailableUnits() ? "Borrow Item" : "Check Dates" %></a>
                            <% } else { %>
                                <span class="btn btn-outline btn-sm disabled">Currently Unavailable</span>
                            <% } %>
                        </div>

                        <% if (u != null && u.isAdmin()) { %>
                            <div class="item-card-admin-actions">
                                <a href="${pageContext.request.contextPath}/items?action=edit&id=<%= item.getId() %>"
                                   class="btn btn-outline btn-sm">Edit</a>
                                <form method="post" action="${pageContext.request.contextPath}/items"
                                      class="inline-form"
                                      data-confirm-title="Delete item?"
                                      data-confirm-message="This will permanently remove <%= item.getName() %> from the catalogue."
                                      data-confirm-button="Delete Item"
                                      data-confirm-tone="danger">
                                    <input type="hidden" name="action" value="delete">
                                    <input type="hidden" name="id" value="<%= item.getId() %>">
                                    <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                                </form>
                            </div>
                        <% } %>
                    </div>
                </article>
            <% } %>
        </section>
    <% } %>
</main>

<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>


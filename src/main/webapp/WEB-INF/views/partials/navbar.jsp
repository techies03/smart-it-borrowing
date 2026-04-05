<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartit.model.User" %>
<%
    User navUser = (User) session.getAttribute("loggedInUser");
    String ctx = request.getContextPath();
    String path = request.getServletPath();
    String initial = (navUser != null && navUser.getName() != null && !navUser.getName().isEmpty())
            ? navUser.getName().substring(0, 1)
            : "?";
%>
<nav class="navbar">
    <a href="<%= ctx %>/items" class="nav-brand">
        <span class="brand-mark">SI</span>
        <span class="brand-copy">
            <strong>Smart IT Borrowing</strong>
            <small>Equipment Desk</small>
        </span>
    </a>

    <button class="nav-hamburger" id="nav-hamburger" aria-label="Toggle menu">
        <span></span>
        <span></span>
        <span></span>
    </button>

    <ul class="nav-links" id="nav-links">
        <li><a href="<%= ctx %>/items" class="<%= path.startsWith("/items") ? "active" : "" %>">Catalogue</a></li>
        <li><a href="<%= ctx %>/bookings" class="<%= path.startsWith("/bookings") ? "active" : "" %>">Bookings</a></li>
        <% if (navUser != null && navUser.isAdmin()) { %>
            <li><a href="<%= ctx %>/admin/dashboard" class="<%= path.startsWith("/admin/dashboard") ? "active" : "" %>">Dashboard</a></li>
            <li><a href="<%= ctx %>/admin/bookings" class="<%= path.startsWith("/admin/bookings") ? "active" : "" %>">Operations</a></li>
        <% } %>
    </ul>

    <div class="nav-right">
        <button class="theme-toggle" id="theme-toggle-btn" onclick="toggleTheme()" title="Toggle theme">☾</button>
        <div class="nav-user-pill">
            <div class="nav-avatar"><%= initial %></div>
            <div class="nav-user-copy">
                <span class="nav-user-name"><%= navUser != null ? navUser.getName() : "" %></span>
                <span class="nav-user-role"><%= navUser != null && navUser.isAdmin() ? "Administrator" : "Borrower" %></span>
            </div>
        </div>
        <a href="<%= ctx %>/logout" class="btn btn-outline btn-sm">Logout</a>
    </div>
</nav>
<% if (navUser != null && navUser.isAdmin()) { %>
<div id="confirm-modal" class="modal-overlay" style="display:none">
    <div class="modal-box">
        <span class="eyebrow">Admin Confirmation</span>
        <h3 id="confirm-title">Confirm action</h3>
        <p id="confirm-message" class="section-note">Proceed with this admin action?</p>
        <div id="confirm-damage-fee-group" class="form-group" style="display:none">
            <label for="confirm-damage-fee-input">Damage Fee (RM)</label>
            <input type="number" id="confirm-damage-fee-input" class="form-control" min="0" step="0.01" value="0.00">
            <p id="confirm-damage-fee-note" class="form-hint">Add the additional damage charge to be combined with any late fee.</p>
        </div>
        <div class="confirm-actions">
            <button type="button" class="btn btn-outline" onclick="hideConfirmModal()">Cancel</button>
            <button type="button" class="btn btn-primary" id="confirm-submit-btn">Confirm</button>
        </div>
    </div>
</div>
<% } %>

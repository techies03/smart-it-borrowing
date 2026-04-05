<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login — Smart IT Borrowing</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="auth-page">

<main class="auth-layout">
    <section class="auth-aside">
        <div>
            <div class="auth-brand">
                <span class="brand-mark">SI</span>
                <span class="auth-brand-copy">
                    <strong>Smart IT Borrowing</strong>
                    <small>Equipment booking system</small>
                </span>
            </div>

            <span class="kicker mt-2">Welcome Back</span>
            <h1>Sign in to Smart IT Borrowing.</h1>
            <p>Sign in to browse equipment, submit requests, or manage bookings.</p>
        </div>

        <div class="auth-points">
            <div class="auth-point">
                <strong>Borrowers</strong>
                <span>Browse items, check stock, and send requests.</span>
            </div>
            <div class="auth-point">
                <strong>Admins</strong>
                <span>Review approvals, returns, and inventory.</span>
            </div>
        </div>

        <div class="auth-credentials">
            <p>Demo Sign-In</p>
            <div class="credential-row">
                <span>Admin</span>
                <strong>admin@smartit.com / admin123</strong>
            </div>
            <div class="credential-row">
                <span>User</span>
                <strong>john@smartit.com / admin123</strong>
            </div>
        </div>
    </section>

    <section class="auth-card">
        <div class="auth-card-header">
            <span class="eyebrow">Sign In</span>
            <h2>Sign in</h2>
            <p>Enter your email and password.</p>
        </div>

        <% String registered = request.getParameter("registered"); %>
        <% if ("true".equals(registered)) { %>
            <div class="alert alert-success">Account created. You can sign in now.</div>
        <% } %>
        <% String logout = request.getParameter("logout"); %>
        <% if ("true".equals(logout)) { %>
            <div class="alert alert-info">You have been logged out.</div>
        <% } %>
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-danger">${error}</div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/login" novalidate>
            <div class="form-group">
                <label for="email">Email Address</label>
                <input type="email" id="email" name="email" class="form-control"
                       placeholder="you@example.com" required autofocus>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <div class="password-wrapper">
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="••••••••" required>
                    <button type="button" class="password-toggle" onclick="togglePassword('password')">◐</button>
                </div>
            </div>

            <button type="submit" class="btn btn-primary btn-block">Sign In</button>
        </form>

        <p class="auth-footer">
            Need an account?
            <a href="${pageContext.request.contextPath}/register">Create one</a>
        </p>
    </section>
</main>

<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>

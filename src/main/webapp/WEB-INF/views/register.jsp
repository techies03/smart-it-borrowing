<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register — TrackIT</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="auth-page">

<main class="auth-layout">
    <section class="auth-aside">
        <div>
            <div class="auth-brand">
                <span class="brand-mark">TI</span>
                <span class="auth-brand-copy">
                    <strong>TrackIT</strong>
                    <small>User registration</small>
                </span>
            </div>

            <span class="kicker mt-2">Create Account</span>
            <h1>Create your account.</h1>
            <p>Register once, then sign in to browse equipment and submit requests.</p>
        </div>

        <div class="auth-points">
            <div class="auth-point">
                <strong>Account details</strong>
                <span>Use your name, email, and a password with at least 6 characters.</span>
            </div>
            <div class="auth-point">
                <strong>After registration</strong>
                <span>Sign in and start sending booking requests.</span>
            </div>
        </div>
    </section>

    <section class="auth-card">
        <div class="auth-card-header">
            <span class="eyebrow">Registration</span>
            <h2>Register</h2>
            <p>Use your name, email, and password to create an account.</p>
        </div>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-danger">${error}</div>
        <% } %>

        <form method="post" action="${pageContext.request.contextPath}/register" novalidate>
            <div class="form-group">
                <label for="name">Full Name</label>
                <input type="text" id="name" name="name" class="form-control"
                       placeholder="Jane Smith" required autofocus>
            </div>

            <div class="form-group">
                <label for="email">Email Address</label>
                <input type="email" id="email" name="email" class="form-control"
                       placeholder="you@example.com" required>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <div class="password-wrapper">
                    <input type="password" id="password" name="password" class="form-control"
                           placeholder="At least 6 characters" required minlength="6">
                    <button type="button" class="password-toggle" onclick="togglePassword('password')">◐</button>
                </div>
            </div>

            <div class="form-group">
                <label for="confirmPassword">Confirm Password</label>
                <div class="password-wrapper">
                    <input type="password" id="confirmPassword" name="confirmPassword" class="form-control"
                           placeholder="Repeat password" required>
                    <button type="button" class="password-toggle" onclick="togglePassword('confirmPassword')">◐</button>
                </div>
            </div>

            <button type="submit" class="btn btn-primary btn-block">Create Account</button>
        </form>

        <p class="auth-footer">
            Already registered?
            <a href="${pageContext.request.contextPath}/login">Sign in</a>
        </p>
    </section>
</main>

<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>

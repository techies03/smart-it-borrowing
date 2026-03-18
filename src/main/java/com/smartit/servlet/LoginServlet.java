package com.smartit.servlet;

import com.smartit.model.User;
import com.smartit.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * LoginServlet — Handles GET (show login form) and POST (authenticate).
 * On success: redirects ADMIN to /admin/dashboard, USER to /items
 * On failure: returns to login.jsp with error message
 */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // If already logged in, skip login page
        HttpSession session = req.getSession(false);
        if (session != null && session.getAttribute("loggedInUser") != null) {
            User user = (User) session.getAttribute("loggedInUser");
            redirectByRole(user, req, resp);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String email    = req.getParameter("email");
        String password = req.getParameter("password");

        // Basic blank-field check
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            req.setAttribute("error", "Email and password are required.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
            return;
        }

        try {
            User user = userService.login(email.trim(), password);

            // Create new session + store user
            HttpSession session = req.getSession(true);
            session.setAttribute("loggedInUser", user);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            redirectByRole(user, req, resp);

        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);

        } catch (SQLException e) {
            req.setAttribute("error", "A database error occurred. Please try again later.");
            req.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(req, resp);
        }
    }

    private void redirectByRole(User user, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String base = req.getContextPath();
        if (user.isAdmin()) {
            resp.sendRedirect(base + "/admin/dashboard");
        } else {
            resp.sendRedirect(base + "/items");
        }
    }
}

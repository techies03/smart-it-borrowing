package com.smartit.filter;

import com.smartit.model.User;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * AuthFilter — Intercepts all requests to protected pages.
 *
 * Public URLs (no login required): /login, /logout, /register, /error/*
 * Admin-only URLs: /admin/*, checked against session role.
 *
 * On access denied: redirects to login.jsp with an error message.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri          = request.getRequestURI();
        String contextPath  = request.getContextPath();
        String relativePath = uri.substring(contextPath.length());

        // ---- Allow static resources and public pages ----
        if (isPublicResource(relativePath)) {
            chain.doFilter(req, res);
            return;
        }

        // ---- Check session ----
        HttpSession session  = request.getSession(false);
        User        loggedIn = (session != null) ? (User) session.getAttribute("loggedInUser") : null;

        if (loggedIn == null) {
            response.sendRedirect(contextPath + "/login");
            return;
        }

        // ---- Admin-only path guard ----
        if (relativePath.startsWith("/admin") && !loggedIn.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Access denied. Admin privileges are required.");
            return;
        }

        chain.doFilter(req, res);
    }

    /** Resources that don't require authentication */
    private boolean isPublicResource(String path) {
        return path.equals("/login")
            || path.equals("/logout")
            || path.equals("/register")
            || path.startsWith("/static/")
            || path.startsWith("/css/")
            || path.startsWith("/js/")
            || path.startsWith("/images/")
            || path.startsWith("/error/");
    }
}

package com.smartit.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.smartit.dao.UserDAO;
import com.smartit.model.User;

import java.sql.SQLException;
import java.util.Optional;

/**
 * UserService — Business logic for authentication and user management.
 * Uses patrickfav/bcrypt for secure password hashing and verification.
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * Authenticate a user by email and plain-text password.
     *
     * @param email    user's email
     * @param password plain-text password from login form
     * @return the authenticated User if credentials are valid
     * @throws IllegalArgumentException if credentials are invalid
     * @throws SQLException             on database error
     */
    public User login(String email, String password) throws SQLException {
        Optional<User> userOpt = userDAO.findByEmail(email);

        // User not found
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();

        // BCrypt verification — constant-time comparison, safe against timing attacks
        BCrypt.Result result = BCrypt.verifyer()
                .verify(password.toCharArray(), user.getPasswordHash());

        if (!result.verified) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return user;
    }

    /**
     * Register a new USER-role account (public signup).
     * Hashes the password using BCrypt before storing.
     */
    public User register(String name, String email, String password) throws SQLException {
        // Check email not already in use
        if (userDAO.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email address is already registered.");
        }

        // BCrypt hash — cost factor 12 is the recommended minimum for 2024
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setRole("USER");

        userDAO.create(user);
        return user;
    }
}

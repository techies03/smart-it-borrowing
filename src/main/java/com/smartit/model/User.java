package com.smartit.model;

/**
 * User — Entity model for system users.
 * Maps to the `users` table.
 * Role: ADMIN can manage items/bookings; USER can create bookings.
 */
public class User {

    private int    id;
    private String name;
    private String email;
    private String passwordHash; // BCrypt hash, never store plain text
    private String role;         // "ADMIN" or "USER"

    // --- Constructors ---

    public User() {}

    public User(int id, String name, String email, String passwordHash, String role) {
        this.id           = id;
        this.name         = name;
        this.email        = email;
        this.passwordHash = passwordHash;
        this.role         = role;
    }

    // --- Getters & Setters ---

    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getEmail()              { return email; }
    public void   setEmail(String email)  { this.email = email; }

    public String getPasswordHash()                    { return passwordHash; }
    public void   setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole()             { return role; }
    public void   setRole(String role)  { this.role = role; }

    /** Convenience: returns true if this user has the ADMIN role */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', email='" + email + "', role='" + role + "'}";
    }
}

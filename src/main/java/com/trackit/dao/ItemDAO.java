package com.trackit.dao;

import com.trackit.model.Item;
import com.trackit.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ItemDAO — Data Access Object for the `items` table.
 * Supports full CRUD + search-by-name + filter-by-category.
 */
public class ItemDAO {

    /**
     * Retrieve all items, ordered by name.
     */
    public List<Item> findAll() throws SQLException {
        return queryItems("SELECT id, name, category, quantity, status, item_condition, image_url "
                + "FROM items ORDER BY name", null, null);
    }

    /**
     * Search items whose name contains the keyword (case-insensitive).
     */
    public List<Item> searchByName(String keyword) throws SQLException {
        return queryItems(
            "SELECT id, name, category, quantity, status, item_condition, image_url "
          + "FROM items WHERE name LIKE ? ORDER BY name",
            "%" + keyword + "%", null);
    }

    /**
     * Filter items by exact category.
     */
    public List<Item> filterByCategory(String category) throws SQLException {
        return queryItems(
            "SELECT id, name, category, quantity, status, item_condition, image_url "
          + "FROM items WHERE category = ? ORDER BY name",
            category, null);
    }

    /**
     * Search by name AND filter by category simultaneously.
     */
    public List<Item> searchAndFilter(String keyword, String category) throws SQLException {
        return queryItems(
            "SELECT id, name, category, quantity, status, item_condition, image_url "
          + "FROM items WHERE name LIKE ? AND category = ? ORDER BY name",
            "%" + keyword + "%", category);
    }

    /**
     * Find a single item by primary key.
     */
    public Optional<Item> findById(int id) throws SQLException {
        String sql = "SELECT id, name, category, quantity, status, item_condition, image_url FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Get distinct categories for the filter dropdown.
     */
    public List<String> findAllCategories() throws SQLException {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM items ORDER BY category";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) categories.add(rs.getString("category"));
        }
        return categories;
    }

    /**
     * Count total items in the system.
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM items";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Insert a new item. Sets the generated ID on the provided object.
     */
    public void create(Item item) throws SQLException {
        String sql = "INSERT INTO items (name, category, quantity, status, item_condition, image_url) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getCategory());
            ps.setInt   (3, item.getQuantity());
            ps.setString(4, item.getStatus());
            ps.setString(5, item.getItemCondition());
            ps.setString(6, item.getImageUrl());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setId(keys.getInt(1));
            }
        }
    }

    /**
     * Update an existing item.
     */
    public void update(Item item) throws SQLException {
        String sql = "UPDATE items SET name=?, category=?, quantity=?, status=?, item_condition=?, image_url=? "
                   + "WHERE id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getCategory());
            ps.setInt   (3, item.getQuantity());
            ps.setString(4, item.getStatus());
            ps.setString(5, item.getItemCondition());
            ps.setString(6, item.getImageUrl());
            ps.setInt   (7, item.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Delete an item by ID.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // --- Private helpers ---

    /** Reusable query helper: handles 0, 1, or 2 parameters */
    private List<Item> queryItems(String sql, String param1, String param2) throws SQLException {
        List<Item> results = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (param1 != null) ps.setString(1, param1);
            if (param2 != null) ps.setString(2, param2);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        }
        return results;
    }

    private Item mapRow(ResultSet rs) throws SQLException {
        return new Item(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getInt("quantity"),
            rs.getString("status"),
            rs.getString("item_condition"),
            rs.getString("image_url")
        );
    }
}


package com.smartit.service;

import com.smartit.dao.BookingDAO;
import com.smartit.dao.ItemDAO;
import com.smartit.model.Item;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * ItemService — Business logic for equipment management.
 */
public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();
    private final BookingDAO bookingDAO = new BookingDAO();

    /**
     * Get all items (default sort by name).
     */
    public List<Item> getAllItems() throws SQLException {
        return decorateAvailability(itemDAO.findAll());
    }

    /**
     * Search + filter items.
     * Returns all items if both keyword and category are blank.
     */
    public List<Item> searchAndFilter(String keyword, String category) throws SQLException {
        String normalizedKeyword = normalizeOptionalText(keyword);
        String normalizedCategory = normalizeOptionalText(category);
        boolean hasKeyword  = normalizedKeyword != null;
        boolean hasCategory = normalizedCategory != null;

        if (hasKeyword && hasCategory) {
            return decorateAvailability(itemDAO.searchAndFilter(normalizedKeyword, normalizedCategory));
        } else if (hasKeyword) {
            return decorateAvailability(itemDAO.searchByName(normalizedKeyword));
        } else if (hasCategory) {
            return decorateAvailability(itemDAO.filterByCategory(normalizedCategory));
        } else {
            return decorateAvailability(itemDAO.findAll());
        }
    }

    /**
     * Get item by ID, or throw if not found.
     */
    public Item getById(int id) throws SQLException {
        Item item = itemDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Item with ID " + id + " not found."));
        decorateAvailability(item);
        return item;
    }

    /**
     * All distinct categories for filter dropdown.
     */
    public List<String> getCategories() throws SQLException {
        return itemDAO.findAllCategories();
    }

    /**
     * Create a new item — validates mandatory fields.
     */
    public Item createItem(String name, String category, int quantity, String imageUrl) throws SQLException {
        validate(name, category, quantity);

        Item item = new Item();
        item.setName(name.trim());
        item.setCategory(category.trim());
        item.setQuantity(quantity);
        item.setStatus("AVAILABLE");
        item.setItemCondition("GOOD");
        item.setImageUrl(normalizeImageUrl(imageUrl));

        itemDAO.create(item);
        return item;
    }

    /**
     * Update an existing item.
     */
    public void updateItem(int id, String name, String category, int quantity,
                           String status, String condition, String imageUrl) throws SQLException {
        validate(name, category, quantity);
        String normalizedStatus = normalizeStatus(status);
        String normalizedCondition = normalizeCondition(condition);

        Item item = getById(id);
        item.setName(name.trim());
        item.setCategory(category.trim());
        item.setQuantity(quantity);
        item.setStatus(normalizedStatus);
        item.setItemCondition(normalizedCondition);
        item.setImageUrl(normalizeImageUrl(imageUrl));

        itemDAO.update(item);
    }

    /**
     * Delete an item by ID.
     */
    public void deleteItem(int id) throws SQLException {
        itemDAO.delete(id);
    }

    /**
     * Total item count for the dashboard.
     */
    public int countAll() throws SQLException {
        return itemDAO.countAll();
    }

    // --- Validation ---

    private void validate(String name, String category, int quantity) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name is required.");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category is required.");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }
    }

    private String normalizeStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (!"AVAILABLE".equals(normalized) && !"UNAVAILABLE".equals(normalized)) {
            throw new IllegalArgumentException("Status must be AVAILABLE or UNAVAILABLE.");
        }
        return normalized;
    }

    private String normalizeCondition(String condition) {
        String normalized = condition == null ? "" : condition.trim().toUpperCase();
        if (!"GOOD".equals(normalized) && !"DAMAGED".equals(normalized)) {
            throw new IllegalArgumentException("Condition must be GOOD or DAMAGED.");
        }
        return normalized;
    }

    private String normalizeImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private List<Item> decorateAvailability(List<Item> items) throws SQLException {
        if (items.isEmpty()) {
            return items;
        }

        Map<Integer, Integer> reservedUnits = bookingDAO.countReservedUnits();
        for (Item item : items) {
            item.setReservedUnits(reservedUnits.getOrDefault(item.getId(), 0));
        }
        return items;
    }

    private void decorateAvailability(Item item) throws SQLException {
        item.setReservedUnits(bookingDAO.countReservedUnitsForItem(item.getId()));
    }
}

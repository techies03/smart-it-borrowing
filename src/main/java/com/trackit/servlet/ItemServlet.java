package com.trackit.servlet;

import com.trackit.model.Item;
import com.trackit.model.User;
import com.trackit.service.ItemService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ItemServlet — Handles the item catalog pages displayed to all logged-in users.
 * Users can browse/search/filter items and click "Borrow" to start a booking.
 * Admins additionally see "Add / Edit / Delete" buttons.
 *
 * Routes:
 *   GET  /items            → list all / search-filtered items
 *   GET  /items?action=new → show add-item form (admin only)
 *   GET  /items?action=edit&id=X → show edit form (admin only)
 *   POST /items?action=create → save new item (admin only)
 *   POST /items?action=update → save edited item (admin only)
 *   POST /items?action=delete → delete item (admin only)
 */
@WebServlet("/items")
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 10 * 1024 * 1024)
public class ItemServlet extends HttpServlet {

    private final ItemService itemService = new ItemService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");

        try {
            if ("new".equals(action)) {
                if (!requireAdmin(req, resp)) return; // guard
                req.getRequestDispatcher("/WEB-INF/views/item-form.jsp").forward(req, resp);
                return;
            }

            if ("edit".equals(action)) {
                if (!requireAdmin(req, resp)) return;
                int  id   = Integer.parseInt(req.getParameter("id"));
                Item item = itemService.getById(id);
                req.setAttribute("item", item);
                req.getRequestDispatcher("/WEB-INF/views/item-form.jsp").forward(req, resp);
                return;
            }

            // --- Default: list all / search results ---
            String keyword  = req.getParameter("keyword");
            String category = req.getParameter("category");

            List<Item>   items      = itemService.searchAndFilter(keyword, category);
            List<String> categories = itemService.getCategories();

            req.setAttribute("items",      items);
            req.setAttribute("categories", categories);
            req.setAttribute("keyword",    keyword);
            req.setAttribute("category",   category);
            req.getRequestDispatcher("/WEB-INF/views/item-list.jsp").forward(req, resp);

        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/items");
        } catch (IllegalArgumentException e) {
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/item-list.jsp").forward(req, resp);
        } catch (SQLException e) {
            req.setAttribute("error", "Database error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/item-list.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String action = req.getParameter("action");
        boolean formAction = "create".equals(action) || "update".equals(action);

        try {
            switch (action == null ? "" : action) {

                case "create":
                    assertAdmin(req);
                    itemService.createItem(
                        req.getParameter("name"),
                        req.getParameter("category"),
                        Integer.parseInt(req.getParameter("quantity")),
                        storeItemImage(req)
                    );
                    resp.sendRedirect(req.getContextPath() + "/items?success=created");
                    break;

                case "update":
                    assertAdmin(req);
                    itemService.updateItem(
                        Integer.parseInt(req.getParameter("id")),
                        req.getParameter("name"),
                        req.getParameter("category"),
                        Integer.parseInt(req.getParameter("quantity")),
                        req.getParameter("status"),
                        req.getParameter("itemCondition"),
                        resolveImageUrl(req)
                    );
                    resp.sendRedirect(req.getContextPath() + "/items?success=updated");
                    break;

                case "delete":
                    assertAdmin(req);
                    itemService.deleteItem(Integer.parseInt(req.getParameter("id")));
                    resp.sendRedirect(req.getContextPath() + "/items?success=deleted");
                    break;

                default:
                    resp.sendRedirect(req.getContextPath() + "/items");
            }
        } catch (NumberFormatException e) {
            if (formAction) {
                req.setAttribute("error", "Please enter a valid numeric value.");
                req.setAttribute("item", buildItemFromRequest(req));
                req.getRequestDispatcher("/WEB-INF/views/item-form.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/items");
        } catch (SecurityException e) {
            resp.sendRedirect(req.getContextPath() + "/items");
        } catch (IllegalArgumentException e) {
            if (formAction) {
                req.setAttribute("error", e.getMessage());
                req.setAttribute("item", buildItemFromRequest(req));
                req.getRequestDispatcher("/WEB-INF/views/item-form.jsp").forward(req, resp);
                return;
            }
            resp.sendRedirect(req.getContextPath() + "/items");
        } catch (SQLException e) {
            if (formAction) {
                req.setAttribute("error", "Database error: " + e.getMessage());
                req.setAttribute("item", buildItemFromRequest(req));
                req.getRequestDispatcher("/WEB-INF/views/item-form.jsp").forward(req, resp);
                return;
            }
            req.setAttribute("error", "Database error: " + e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/item-list.jsp").forward(req, resp);
        }
    }

    // --- Role guards ---

    private boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        User user = (User) req.getSession().getAttribute("loggedInUser");
        if (user == null || !user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/items");
            return false;
        }
        return true;
    }

    private void assertAdmin(HttpServletRequest req) {
        User user = (User) req.getSession().getAttribute("loggedInUser");
        if (user == null || !user.isAdmin()) {
            throw new SecurityException("Admin privileges required.");
        }
    }

    private String resolveImageUrl(HttpServletRequest req) throws IOException, ServletException {
        String uploadedImageUrl = storeItemImage(req);
        if (uploadedImageUrl != null) {
            return uploadedImageUrl;
        }

        String existingImageUrl = req.getParameter("existingImageUrl");
        return existingImageUrl == null || existingImageUrl.isBlank() ? null : existingImageUrl.trim();
    }

    private String storeItemImage(HttpServletRequest req) throws IOException, ServletException {
        Part imagePart = req.getPart("image");
        if (imagePart == null || imagePart.getSize() == 0) {
            return null;
        }

        String contentType = imagePart.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be an image.");
        }

        String submittedFileName = imagePart.getSubmittedFileName();
        String extension = extractExtension(submittedFileName);
        String storedFileName = UUID.randomUUID() + extension;

        List<Path> uploadDirectories = new ArrayList<>();
        Path projectUploadDirectory = resolveProjectUploadDirectory();
        if (projectUploadDirectory != null) {
            uploadDirectories.add(projectUploadDirectory);
        }

        Path runtimeUploadDirectory = resolveRuntimeUploadDirectory(req);
        if (runtimeUploadDirectory != null && uploadDirectories.stream().noneMatch(dir -> samePath(dir, runtimeUploadDirectory))) {
            uploadDirectories.add(runtimeUploadDirectory);
        }

        if (uploadDirectories.isEmpty()) {
            throw new IOException("Unable to resolve upload directory.");
        }

        for (Path directory : uploadDirectories) {
            Files.createDirectories(directory);
        }

        Path primaryTarget = uploadDirectories.get(0).resolve(storedFileName);

        try (InputStream inputStream = imagePart.getInputStream()) {
            Files.copy(inputStream, primaryTarget, StandardCopyOption.REPLACE_EXISTING);
        }

        for (int i = 1; i < uploadDirectories.size(); i++) {
            Path mirrorTarget = uploadDirectories.get(i).resolve(storedFileName);
            Files.copy(primaryTarget, mirrorTarget, StandardCopyOption.REPLACE_EXISTING);
        }

        return "/static/uploads/items/" + storedFileName;
    }

    private Path resolveProjectUploadDirectory() {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        if (Files.exists(projectRoot.resolve("pom.xml"))) {
            return projectRoot.resolve(Paths.get("src", "main", "webapp", "static", "uploads", "items"));
        }
        return null;
    }

    private Path resolveRuntimeUploadDirectory(HttpServletRequest req) {
        String realPath = req.getServletContext().getRealPath("/static/uploads/items");
        if (realPath == null || realPath.isBlank()) {
            return null;
        }

        return Paths.get(realPath);
    }

    private String extractExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String safeFileName = Paths.get(fileName).getFileName().toString();
        int dotIndex = safeFileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }

        String extension = safeFileName.substring(dotIndex).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : "";
    }

    private boolean samePath(Path left, Path right) {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private Item buildItemFromRequest(HttpServletRequest req) {
        Item item = new Item();

        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            try {
                item.setId(Integer.parseInt(idParam));
            } catch (NumberFormatException ignored) {
                // Ignore invalid IDs when rebuilding form state.
            }
        }

        String quantityParam = req.getParameter("quantity");
        if (quantityParam != null && !quantityParam.isBlank()) {
            try {
                item.setQuantity(Integer.parseInt(quantityParam));
            } catch (NumberFormatException ignored) {
                item.setQuantity(0);
            }
        }

        item.setName(req.getParameter("name"));
        item.setCategory(req.getParameter("category"));
        item.setStatus(req.getParameter("status") != null ? req.getParameter("status") : "AVAILABLE");
        item.setItemCondition(req.getParameter("itemCondition") != null ? req.getParameter("itemCondition") : "GOOD");
        item.setImageUrl(req.getParameter("existingImageUrl"));
        return item;
    }
}


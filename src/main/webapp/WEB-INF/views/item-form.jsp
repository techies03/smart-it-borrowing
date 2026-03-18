<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartit.model.Item" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <%
        Item item = (Item) request.getAttribute("item");
        boolean isEdit = (item != null && item.getId() > 0);
    %>
    <title><%= isEdit ? "Edit" : "Add" %> Item — Smart IT Borrowing</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body class="app-shell">
<%@ include file="/WEB-INF/views/partials/navbar.jsp" %>

<main class="container container-narrow page-shell">
    <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-danger">${error}</div>
    <% } %>

    <div class="form-shell">
        <section class="surface-panel form-aside">
            <div>
                <span class="kicker">Inventory</span>
                <h2><%= isEdit ? "Update this item." : "Create an item." %></h2>
                <p><%= isEdit ? "Edit the name, stock, image, status, and condition here." : "Enter the name, stock, image, and category here." %></p>
            </div>

            <div class="image-preview-panel <%= item != null && item.getImageUrl() != null && !item.getImageUrl().isBlank() ? "" : "is-empty" %>">
                <img id="item-image-preview"
                     class="item-form-image"
                     data-current-src="<%= item != null && item.getImageUrl() != null ? request.getContextPath() + item.getImageUrl() : "" %>"
                     src="<%= item != null && item.getImageUrl() != null ? request.getContextPath() + item.getImageUrl() : "" %>"
                     alt="Item preview"
                     style="<%= item != null && item.getImageUrl() != null && !item.getImageUrl().isBlank() ? "" : "display:none" %>">
                <span id="item-image-placeholder"
                      class="image-preview-placeholder"
                      style="<%= item != null && item.getImageUrl() != null && !item.getImageUrl().isBlank() ? "display:none" : "" %>">
                    Upload an image to preview it here.
                </span>
            </div>

            <div class="helper-list">
                <div class="helper-item">
                    <span>Mode</span>
                    <strong><%= isEdit ? "Editing Existing Item" : "Creating New Item" %></strong>
                </div>
                <div class="helper-item">
                    <span>Current Status</span>
                    <strong><%= item != null && item.getStatus() != null ? item.getStatus() : "AVAILABLE" %></strong>
                </div>
                <div class="helper-item">
                    <span>Condition</span>
                    <strong><%= item != null && item.getItemCondition() != null ? item.getItemCondition() : "GOOD" %></strong>
                </div>
            </div>
        </section>

        <section class="surface-panel form-panel">
            <div class="form-panel-header">
                <h3><%= isEdit ? "Edit Equipment" : "Add Equipment" %></h3>
                <p class="section-note">Enter the item details and stock count.</p>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/items" enctype="multipart/form-data"
                  data-confirm-title="<%= isEdit ? "Save equipment changes?" : "Add this equipment?" %>"
                  data-confirm-message="<%= isEdit ? "This will save the updated item details." : "This will add a new item to the catalogue." %>"
                  data-confirm-button="<%= isEdit ? "Save Changes" : "Add Equipment" %>"
                  data-confirm-tone="primary">
                <input type="hidden" name="action" value="<%= isEdit ? "update" : "create" %>">
                <% if (isEdit) { %>
                    <input type="hidden" name="id" value="<%= item.getId() %>">
                <% } %>
                <input type="hidden" name="existingImageUrl" value="<%= item != null && item.getImageUrl() != null ? item.getImageUrl() : "" %>">

                <div class="form-group">
                    <label for="name">Equipment Name</label>
                    <input type="text" id="name" name="name" class="form-control"
                           value="<%= item != null && item.getName() != null ? item.getName() : "" %>" required>
                </div>

                <div class="field-grid">
                    <div class="form-group">
                        <label for="category">Category</label>
                        <input type="text" id="category" name="category" class="form-control"
                               value="<%= item != null && item.getCategory() != null ? item.getCategory() : "" %>"
                               list="category-options" required>
                        <datalist id="category-options">
                            <option value="Laptop">
                            <option value="Desktop">
                            <option value="Monitor">
                            <option value="Projector">
                            <option value="Keyboard">
                            <option value="Mouse">
                            <option value="Printer">
                            <option value="Scanner">
                            <option value="Networking">
                            <option value="Accessory">
                            <option value="Other">
                        </datalist>
                    </div>

                    <div class="form-group">
                        <label for="quantity">Quantity</label>
                        <input type="number" id="quantity" name="quantity" class="form-control"
                               value="<%= item != null ? item.getQuantity() : 1 %>" min="0" required>
                        <p class="form-hint">Set the number of borrowable units available under this catalogue entry. Use 0 when stock is fully depleted.</p>
                    </div>
                </div>

                <div class="form-group">
                    <label for="image">Item Image</label>
                    <div class="file-upload">
                        <input type="file"
                               id="image"
                               name="image"
                               class="file-upload-input"
                               accept="image/*"
                               aria-describedby="image-file-name image-hint">
                        <div class="file-upload-surface" aria-hidden="true">
                            <div class="file-upload-copy">
                                <span class="file-upload-title">Choose an image for this item</span>
                                <span id="image-file-name" class="file-upload-file-name">
                                    <%= item != null && item.getImageUrl() != null && !item.getImageUrl().isBlank()
                                            ? "Current image is still active. Choose a new file to replace it."
                                            : "No image selected yet." %>
                                </span>
                            </div>
                            <span class="file-upload-button">Browse Files</span>
                        </div>
                    </div>
                    <p id="image-hint" class="form-hint">Upload a clear image up to 5 MB. It will be used in the catalogue and booking summary.</p>
                </div>

                <% if (isEdit) { %>
                    <div class="field-grid">
                        <div class="form-group">
                            <label for="status">Status</label>
                            <div class="select-shell">
                                <select id="status" name="status" class="form-control">
                                    <option value="AVAILABLE" <%= "AVAILABLE".equals(item.getStatus()) ? "selected" : "" %>>Available</option>
                                    <option value="UNAVAILABLE" <%= "UNAVAILABLE".equals(item.getStatus()) ? "selected" : "" %>>Unavailable</option>
                                </select>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="itemCondition">Condition</label>
                            <div class="select-shell">
                                <select id="itemCondition" name="itemCondition" class="form-control">
                                    <option value="GOOD" <%= "GOOD".equals(item.getItemCondition()) ? "selected" : "" %>>Good</option>
                                    <option value="DAMAGED" <%= "DAMAGED".equals(item.getItemCondition()) ? "selected" : "" %>>Damaged</option>
                                </select>
                            </div>
                        </div>
                    </div>
                <% } %>

                <div class="form-actions">
                    <button type="submit" class="btn btn-primary"><%= isEdit ? "Save Changes" : "Add Equipment" %></button>
                    <a href="${pageContext.request.contextPath}/items" class="btn btn-outline">Back To Catalogue</a>
                </div>
            </form>
        </section>
    </div>
</main>

<script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>

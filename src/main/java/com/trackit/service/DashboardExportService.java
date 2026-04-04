package com.trackit.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.trackit.dao.UserDAO;
import com.trackit.model.Booking;
import com.trackit.model.CategoryStockSnapshot;
import com.trackit.model.DashboardReportData;
import com.trackit.model.DashboardTrendPoint;
import com.trackit.model.Item;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardExportService — Builds report data and renders XLSX/PDF exports.
 */
public class DashboardExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final String INVENTORY_NOTE =
            "Inventory figures reflect the current stock snapshot at export time. Booking metrics use borrow dates within the selected range.";

    private final ItemService itemService = new ItemService();
    private final BookingService bookingService = new BookingService();
    private final UserDAO userDAO = new UserDAO();

    public DashboardReportData buildReportData(LocalDate fromDate, LocalDate toDate) throws SQLException {
        validateDateRange(fromDate, toDate);

        List<Item> items = itemService.getAllItems();
        List<Booking> bookings = filterBookingsByBorrowDate(bookingService.getAllBookings(), fromDate, toDate);
        LocalDate today = LocalDate.now();

        int activeBookings = 0;
        int overdueBookings = 0;
        int pendingBookings = 0;
        int returnPendingBookings = 0;
        int returnedBookings = 0;
        int rejectedBookings = 0;
        int dueToday = 0;

        for (Booking booking : bookings) {
            String status = booking.getStatus() != null ? booking.getStatus().toUpperCase() : "";
            switch (status) {
                case "PENDING" -> pendingBookings++;
                case "APPROVED" -> {
                    activeBookings++;
                    if (booking.getReturnDate() != null && booking.getReturnDate().toLocalDate().isBefore(today)) {
                        overdueBookings++;
                    }
                    if (booking.getReturnDate() != null && booking.getReturnDate().toLocalDate().isEqual(today)) {
                        dueToday++;
                    }
                }
                case "RETURN_PENDING" -> {
                    activeBookings++;
                    returnPendingBookings++;
                }
                case "RETURNED" -> returnedBookings++;
                case "REJECTED" -> rejectedBookings++;
                default -> {
                    // ignore unknown states in summary counts
                }
            }
        }

        int totalUnits = items.stream().mapToInt(Item::getQuantity).sum();
        int reservedUnits = items.stream().mapToInt(Item::getReservedUnits).sum();
        int availableUnits = items.stream().mapToInt(Item::getAvailableUnits).sum();

        return new DashboardReportData(
                fromDate,
                toDate,
                LocalDateTime.now(),
                items.size(),
                totalUnits,
                availableUnits,
                reservedUnits,
                bookings.size(),
                activeBookings,
                overdueBookings,
                pendingBookings,
                returnPendingBookings,
                returnedBookings,
                rejectedBookings,
                userDAO.countAll(),
                dueToday,
                buildBookingTrend(bookings, fromDate, toDate),
                buildCategorySnapshots(items),
                bookings
        );
    }

    public void writeSummaryXlsx(DashboardReportData data, OutputStream outputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);

            createSummarySheet(workbook, data, titleStyle, headerStyle, sectionStyle, bodyStyle, numberStyle);
            createCategoryStockSheet(workbook, data, titleStyle, headerStyle, bodyStyle, numberStyle);
            createTrendSheet(workbook, data, titleStyle, headerStyle, bodyStyle, numberStyle);
            workbook.write(outputStream);
        }
    }

    public void writeBookingDataXlsx(DashboardReportData data, OutputStream outputStream) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle numberStyle = createNumberStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            Sheet sheet = workbook.createSheet("Booking Data");
            int rowIndex = 0;

            rowIndex = addSheetTitle(sheet, rowIndex, "Booking Data Export", titleStyle);
            rowIndex = addMetaRow(sheet, rowIndex, "Borrow Date Range", formatPeriod(data), bodyStyle);
            rowIndex = addMetaRow(sheet, rowIndex, "Generated At", formatDateTime(data.getGeneratedAt()), bodyStyle);
            rowIndex = addMetaRow(sheet, rowIndex, "Record Count", data.getBookings().size(), numberStyle);
            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {
                    "Reference", "User", "Item", "Quantity", "Borrow Date", "Return Date",
                    "Actual Return", "Status", "Penalty (RM)", "Condition Before", "Condition After"
            };
            for (int i = 0; i < headers.length; i++) {
                createCell(headerRow, i, headers[i], headerStyle);
            }

            for (Booking booking : data.getBookings()) {
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                createCell(row, col++, booking.getReferenceCode(), bodyStyle);
                createCell(row, col++, safeText(booking.getUserName()), bodyStyle);
                createCell(row, col++, safeText(booking.getItemName()), bodyStyle);
                createCell(row, col++, booking.getQuantity(), numberStyle);
                createCell(row, col++, formatDateTime(booking.getBorrowDate()), bodyStyle);
                createCell(row, col++, formatDateTime(booking.getReturnDate()), bodyStyle);
                createCell(row, col++, formatDateTime(booking.getActualReturnDate()), bodyStyle);
                createCell(row, col++, safeText(booking.getStatus()), bodyStyle);
                createCell(row, col++, currencyValue(booking.getPenalty()), currencyStyle);
                createCell(row, col++, safeText(booking.getConditionBefore()), bodyStyle);
                createCell(row, col, safeText(booking.getConditionAfter()), bodyStyle);
            }

            if (data.getBookings().isEmpty()) {
                Row row = sheet.createRow(rowIndex);
                createCell(row, 0, "No bookings were found for the selected borrow-date range.", bodyStyle);
            }

            applyColumnWidths(sheet, new int[] { 4200, 5600, 6200, 2600, 5200, 5200, 5200, 3600, 3600, 3600, 3600 });
            sheet.createFreezePane(0, 5);
            workbook.write(outputStream);
        }
    }

    public void writeSummaryPdf(DashboardReportData data, OutputStream outputStream) throws IOException {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD, new Color(20, 48, 79));
            com.lowagie.text.Font headingFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 13, com.lowagie.text.Font.BOLD, new Color(36, 48, 66));
            com.lowagie.text.Font bodyFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, new Color(36, 48, 66));
            com.lowagie.text.Font smallFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, new Color(109, 122, 138));

            Paragraph title = new Paragraph("Dashboard Summary Export", titleFont);
            title.setSpacingAfter(10f);
            document.add(title);
            document.add(new Paragraph("Borrow Date Range: " + formatPeriod(data), bodyFont));
            document.add(new Paragraph("Generated At: " + formatDateTime(data.getGeneratedAt()), bodyFont));
            Paragraph note = new Paragraph(INVENTORY_NOTE, smallFont);
            note.setSpacingAfter(14f);
            document.add(note);

            document.add(sectionParagraph("Current Inventory Snapshot", headingFont));
            document.add(createMetricPdfTable(
                    new String[][] {
                            { "Equipment Entries", String.valueOf(data.getTotalItems()) },
                            { "Total Units", String.valueOf(data.getTotalUnits()) },
                            { "Available Units", String.valueOf(data.getAvailableUnits()) },
                            { "Reserved Units", String.valueOf(data.getReservedUnits()) },
                            { "Registered Users", String.valueOf(data.getTotalUsers()) }
                    },
                    bodyFont
            ));

            document.add(sectionParagraph("Booking Activity for Selected Period", headingFont));
            document.add(createMetricPdfTable(
                    new String[][] {
                            { "Total Bookings", String.valueOf(data.getTotalBookings()) },
                            { "Pending Approvals", String.valueOf(data.getPendingBookings()) },
                            { "Approved / Active", String.valueOf(data.getApprovedBookings()) },
                            { "Awaiting Return Check", String.valueOf(data.getReturnPendingBookings()) },
                            { "Completed Returns", String.valueOf(data.getReturnedBookings()) },
                            { "Rejected Requests", String.valueOf(data.getRejectedBookings()) },
                            { "Overdue Bookings", String.valueOf(data.getOverdueBookings()) },
                            { "Due Today", String.valueOf(data.getDueToday()) }
                    },
                    bodyFont
            ));

            document.add(sectionParagraph("Category Stock Snapshot", headingFont));
            PdfPTable categoryTable = new PdfPTable(new float[] { 3.2f, 1.2f, 1.2f, 1.2f });
            categoryTable.setWidthPercentage(100);
            addPdfHeaderCell(categoryTable, "Category");
            addPdfHeaderCell(categoryTable, "Total");
            addPdfHeaderCell(categoryTable, "Reserved");
            addPdfHeaderCell(categoryTable, "Available");
            for (CategoryStockSnapshot snapshot : data.getCategoryStock()) {
                addPdfBodyCell(categoryTable, snapshot.getCategory(), bodyFont);
                addPdfBodyCell(categoryTable, String.valueOf(snapshot.getTotalUnits()), bodyFont);
                addPdfBodyCell(categoryTable, String.valueOf(snapshot.getReservedUnits()), bodyFont);
                addPdfBodyCell(categoryTable, String.valueOf(snapshot.getAvailableUnits()), bodyFont);
            }
            if (data.getCategoryStock().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No category stock data available.", bodyFont));
                emptyCell.setColspan(4);
                emptyCell.setPadding(8f);
                categoryTable.addCell(emptyCell);
            }
            categoryTable.setSpacingAfter(12f);
            document.add(categoryTable);

            document.add(sectionParagraph("Daily Booking Trend", headingFont));
            PdfPTable trendTable = new PdfPTable(new float[] { 2.2f, 1.2f });
            trendTable.setWidthPercentage(60);
            addPdfHeaderCell(trendTable, "Date");
            addPdfHeaderCell(trendTable, "Bookings");
            for (DashboardTrendPoint point : data.getBookingTrend()) {
                addPdfBodyCell(trendTable, point.getLabel(), bodyFont);
                addPdfBodyCell(trendTable, String.valueOf(point.getValue()), bodyFont);
            }
            if (data.getBookingTrend().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No booking activity in the selected range.", bodyFont));
                emptyCell.setColspan(2);
                emptyCell.setPadding(8f);
                trendTable.addCell(emptyCell);
            }
            document.add(trendTable);
        } catch (DocumentException e) {
            throw new IOException("Unable to generate PDF export.", e);
        } finally {
            document.close();
        }
    }

    public void writeBookingDataPdf(DashboardReportData data, OutputStream outputStream) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 28, 28);
        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, new Color(20, 48, 79));
            com.lowagie.text.Font bodyFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8.5f, com.lowagie.text.Font.NORMAL, new Color(36, 48, 66));

            Paragraph title = new Paragraph("Booking Data Export", titleFont);
            title.setSpacingAfter(8f);
            document.add(title);
            document.add(new Paragraph("Borrow Date Range: " + formatPeriod(data), bodyFont));
            document.add(new Paragraph("Generated At: " + formatDateTime(data.getGeneratedAt()), bodyFont));
            document.add(new Paragraph("Record Count: " + data.getBookings().size(), bodyFont));
            document.add(new Paragraph(" ", bodyFont));

            PdfPTable table = new PdfPTable(new float[] { 1.15f, 1.75f, 2.25f, 0.8f, 1.55f, 1.55f, 1.15f, 1.05f });
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "Ref");
            addPdfHeaderCell(table, "User");
            addPdfHeaderCell(table, "Item");
            addPdfHeaderCell(table, "Qty");
            addPdfHeaderCell(table, "Borrow");
            addPdfHeaderCell(table, "Return");
            addPdfHeaderCell(table, "Status");
            addPdfHeaderCell(table, "Fee");

            for (Booking booking : data.getBookings()) {
                addPdfBodyCell(table, booking.getReferenceCode(), bodyFont);
                addPdfBodyCell(table, safeText(booking.getUserName()), bodyFont);
                addPdfBodyCell(table, safeText(booking.getItemName()), bodyFont);
                addPdfBodyCell(table, String.valueOf(booking.getQuantity()), bodyFont);
                addPdfBodyCell(table, formatDateTime(booking.getBorrowDate()), bodyFont);
                addPdfBodyCell(table, formatDateTime(booking.getReturnDate()), bodyFont);
                addPdfBodyCell(table, safeText(booking.getStatus()), bodyFont);
                addPdfBodyCell(table, currencyLabel(booking.getPenalty()), bodyFont);
            }

            if (data.getBookings().isEmpty()) {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No bookings were found for the selected borrow-date range.", bodyFont));
                emptyCell.setColspan(8);
                emptyCell.setPadding(8f);
                table.addCell(emptyCell);
            }

            document.add(table);
        } catch (DocumentException e) {
            throw new IOException("Unable to generate PDF export.", e);
        } finally {
            document.close();
        }
    }

    private void createSummarySheet(XSSFWorkbook workbook,
                                    DashboardReportData data,
                                    CellStyle titleStyle,
                                    CellStyle headerStyle,
                                    CellStyle sectionStyle,
                                    CellStyle bodyStyle,
                                    CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Summary");
        int rowIndex = 0;

        rowIndex = addSheetTitle(sheet, rowIndex, "Dashboard Summary Export", titleStyle);
        rowIndex = addMetaRow(sheet, rowIndex, "Borrow Date Range", formatPeriod(data), bodyStyle);
        rowIndex = addMetaRow(sheet, rowIndex, "Generated At", formatDateTime(data.getGeneratedAt()), bodyStyle);
        rowIndex = addMetaRow(sheet, rowIndex, "Note", INVENTORY_NOTE, bodyStyle);
        rowIndex++;

        rowIndex = addSectionHeader(sheet, rowIndex, "Current Inventory Snapshot", sectionStyle);
        rowIndex = addMetricHeader(sheet, rowIndex, headerStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Equipment Entries", data.getTotalItems(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Total Units", data.getTotalUnits(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Available Units", data.getAvailableUnits(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Reserved Units", data.getReservedUnits(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Registered Users", data.getTotalUsers(), bodyStyle, numberStyle);
        rowIndex++;

        rowIndex = addSectionHeader(sheet, rowIndex, "Booking Activity for Selected Period", sectionStyle);
        rowIndex = addMetricHeader(sheet, rowIndex, headerStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Total Bookings", data.getTotalBookings(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Pending Approvals", data.getPendingBookings(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Approved / Active", data.getApprovedBookings(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Awaiting Return Check", data.getReturnPendingBookings(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Completed Returns", data.getReturnedBookings(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Rejected Requests", data.getRejectedBookings(), bodyStyle, numberStyle);
        rowIndex = addMetricRow(sheet, rowIndex, "Overdue Bookings", data.getOverdueBookings(), bodyStyle, numberStyle);
        addMetricRow(sheet, rowIndex, "Due Today", data.getDueToday(), bodyStyle, numberStyle);

        applyColumnWidths(sheet, new int[] { 9000, 4200 });
    }

    private void createCategoryStockSheet(XSSFWorkbook workbook,
                                          DashboardReportData data,
                                          CellStyle titleStyle,
                                          CellStyle headerStyle,
                                          CellStyle bodyStyle,
                                          CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Category Stock");
        int rowIndex = 0;

        rowIndex = addSheetTitle(sheet, rowIndex, "Category Stock Snapshot", titleStyle);
        rowIndex = addMetaRow(sheet, rowIndex, "Generated At", formatDateTime(data.getGeneratedAt()), bodyStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, "Category", headerStyle);
        createCell(headerRow, 1, "Total Units", headerStyle);
        createCell(headerRow, 2, "Reserved Units", headerStyle);
        createCell(headerRow, 3, "Available Units", headerStyle);

        for (CategoryStockSnapshot snapshot : data.getCategoryStock()) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, snapshot.getCategory(), bodyStyle);
            createCell(row, 1, snapshot.getTotalUnits(), numberStyle);
            createCell(row, 2, snapshot.getReservedUnits(), numberStyle);
            createCell(row, 3, snapshot.getAvailableUnits(), numberStyle);
        }

        if (data.getCategoryStock().isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, 0, "No category stock data available.", bodyStyle);
        }

        applyColumnWidths(sheet, new int[] { 7000, 3600, 3600, 3600 });
    }

    private void createTrendSheet(XSSFWorkbook workbook,
                                  DashboardReportData data,
                                  CellStyle titleStyle,
                                  CellStyle headerStyle,
                                  CellStyle bodyStyle,
                                  CellStyle numberStyle) {
        Sheet sheet = workbook.createSheet("Booking Trend");
        int rowIndex = 0;

        rowIndex = addSheetTitle(sheet, rowIndex, "Daily Booking Trend", titleStyle);
        rowIndex = addMetaRow(sheet, rowIndex, "Borrow Date Range", formatPeriod(data), bodyStyle);
        rowIndex++;

        Row headerRow = sheet.createRow(rowIndex++);
        createCell(headerRow, 0, "Date", headerStyle);
        createCell(headerRow, 1, "Bookings", headerStyle);

        for (DashboardTrendPoint point : data.getBookingTrend()) {
            Row row = sheet.createRow(rowIndex++);
            createCell(row, 0, point.getLabel(), bodyStyle);
            createCell(row, 1, point.getValue(), numberStyle);
        }

        if (data.getBookingTrend().isEmpty()) {
            Row row = sheet.createRow(rowIndex);
            createCell(row, 0, "No booking activity in the selected range.", bodyStyle);
        }

        applyColumnWidths(sheet, new int[] { 5000, 3200 });
    }

    private int addSheetTitle(Sheet sheet, int rowIndex, String title, CellStyle titleStyle) {
        Row row = sheet.createRow(rowIndex++);
        createCell(row, 0, title, titleStyle);
        return rowIndex;
    }

    private int addMetaRow(Sheet sheet, int rowIndex, String label, Object value, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex++);
        createCell(row, 0, label, valueStyle);
        createCell(row, 1, value, valueStyle);
        return rowIndex;
    }

    private int addSectionHeader(Sheet sheet, int rowIndex, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        createCell(row, 0, title, style);
        return rowIndex;
    }

    private int addMetricHeader(Sheet sheet, int rowIndex, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        createCell(row, 0, "Metric", style);
        createCell(row, 1, "Value", style);
        return rowIndex;
    }

    private int addMetricRow(Sheet sheet, int rowIndex, String label, int value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex++);
        createCell(row, 0, label, labelStyle);
        createCell(row, 1, value, valueStyle);
        return rowIndex;
    }

    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createSectionStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createBodyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private CellStyle createNumberStyle(XSSFWorkbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle createCurrencyStyle(XSSFWorkbook workbook) {
        CellStyle style = createBodyStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setDataFormat(workbook.createDataFormat().getFormat("\"RM\" #,##0.00"));
        return style;
    }

    private void createCell(Row row, int columnIndex, Object value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value != null ? value.toString() : "");
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void applyColumnWidths(Sheet sheet, int[] widths) {
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private Paragraph sectionParagraph(String title, com.lowagie.text.Font font) {
        Paragraph paragraph = new Paragraph(title, font);
        paragraph.setSpacingBefore(10f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private PdfPTable createMetricPdfTable(String[][] rows, com.lowagie.text.Font font) {
        PdfPTable table = new PdfPTable(new float[] { 2.3f, 1.2f });
        table.setWidthPercentage(70);
        for (String[] row : rows) {
            addPdfBodyCell(table, row[0], font);
            addPdfBodyCell(table, row[1], font);
        }
        table.setSpacingAfter(8f);
        return table;
    }

    private void addPdfHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(new Color(20, 48, 79));
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addPdfBodyCell(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private List<Booking> filterBookingsByBorrowDate(List<Booking> bookings, LocalDate fromDate, LocalDate toDate) {
        List<Booking> filtered = new ArrayList<>();
        for (Booking booking : bookings) {
            if (booking.getBorrowDate() == null) {
                continue;
            }
            LocalDate borrowDate = booking.getBorrowDate().toLocalDate();
            if (borrowDate.isBefore(fromDate) || borrowDate.isAfter(toDate)) {
                continue;
            }
            filtered.add(booking);
        }

        filtered.sort(Comparator
                .comparing(Booking::getBorrowDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Booking::getId));
        return filtered;
    }

    private List<DashboardTrendPoint> buildBookingTrend(List<Booking> bookings, LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, Integer> countsByDay = new LinkedHashMap<>();
        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            countsByDay.put(cursor, 0);
            cursor = cursor.plusDays(1);
        }

        for (Booking booking : bookings) {
            if (booking.getBorrowDate() == null) {
                continue;
            }
            LocalDate borrowDay = booking.getBorrowDate().toLocalDate();
            if (countsByDay.containsKey(borrowDay)) {
                countsByDay.put(borrowDay, countsByDay.get(borrowDay) + 1);
            }
        }

        List<DashboardTrendPoint> trend = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : countsByDay.entrySet()) {
            trend.add(new DashboardTrendPoint(entry.getKey().format(DATE_FORMAT), entry.getValue()));
        }
        return trend;
    }

    private List<CategoryStockSnapshot> buildCategorySnapshots(List<Item> items) {
        Map<String, int[]> grouped = new LinkedHashMap<>();

        for (Item item : items) {
            String category = item.getCategory() == null || item.getCategory().isBlank()
                    ? "Uncategorized"
                    : item.getCategory().trim();

            int[] totals = grouped.computeIfAbsent(category, ignored -> new int[3]);
            totals[0] += item.getQuantity();
            totals[1] += item.getReservedUnits();
            totals[2] += item.getAvailableUnits();
        }

        List<CategoryStockSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<String, int[]> entry : grouped.entrySet()) {
            int[] totals = entry.getValue();
            snapshots.add(new CategoryStockSnapshot(entry.getKey(), totals[0], totals[1], totals[2]));
        }

        snapshots.sort(Comparator.comparingInt(CategoryStockSnapshot::getReservedUnits)
                .reversed()
                .thenComparing(Comparator.comparingInt(CategoryStockSnapshot::getTotalUnits).reversed())
                .thenComparing(CategoryStockSnapshot::getCategory));

        return snapshots;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Both start and end dates are required.");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("End date must be on or after the start date.");
        }
    }

    private String formatPeriod(DashboardReportData data) {
        return data.getFromDate().format(DATE_FORMAT) + " to " + data.getToDate().format(DATE_FORMAT);
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DATE_TIME_FORMAT) : "-";
    }

    private String currencyLabel(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return "RM " + safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private double currencyValue(BigDecimal amount) {
        BigDecimal safeAmount = amount != null ? amount : BigDecimal.ZERO;
        return safeAmount.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}


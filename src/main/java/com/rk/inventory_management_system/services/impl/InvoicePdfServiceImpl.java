package com.rk.inventory_management_system.services.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.rk.inventory_management_system.entities.Order;
import com.rk.inventory_management_system.services.InvoicePdfService;
import com.rk.inventory_management_system.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final OrderService orderService;

    // Company details
    private static final String COMPANY_NAME = "Aashirwad Company";
    private static final String COMPANY_ADDRESS = "123  XYZ Complex, Dondaicha";
    private static final String COMPANY_CITY = "Dondaicha, Dhule, Maharashtra 400001";
    private static final String COMPANY_PHONE = "Tel: +91-22-1234-5678";
    private static final String COMPANY_EMAIL = "info@ashirwadcompany.com";
    private static final String COMPANY_GST = "GST: 27XXXXX1234X1ZX";

    @Override
    public ByteArrayInputStream generateInvoice(List<Long> orderIds) {
        List<Order> orders = orderService.getOrdersByIds(orderIds);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        Document document = new Document(PageSize.A4, 36, 36, 54, 54);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Add company header
            addCompanyHeader(document);

            // Add invoice info
            addInvoiceInfo(document, dateFormatter);

            // Add some space
            document.add(new Paragraph("\n"));

            BigDecimal grandTotal = BigDecimal.ZERO;

            for (Order order : orders) {
                // Add order details section
                addOrderDetailsSection(document, order, formatter);

                // Add order items table
                addOrderItemsTable(document, order);

                // Add order total
                addOrderTotal(document, order);

                grandTotal = grandTotal.add(BigDecimal.valueOf(order.getTotalPrice()));

                // Add separator between orders
                if (orders.indexOf(order) < orders.size() - 1) {
                    document.add(new Paragraph("\n"));
                    addSeparator(document);
                }
            }

            // Add grand total if multiple orders
            if (orders.size() > 1) {
                addGrandTotal(document, grandTotal);
            }

            // Add footer
            addInvoiceFooter(document);

            document.close();

        } catch (DocumentException exception) {
            log.error("Error occurred during invoice generation", exception);
            throw new RuntimeException("Error Occurred During Invoice Generation", exception);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void addCompanyHeader(Document document) throws DocumentException {
        // Company name
        Font companyNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
        Paragraph companyName = new Paragraph(COMPANY_NAME, companyNameFont);
        companyName.setAlignment(Element.ALIGN_CENTER);
        document.add(companyName);

        // Company details
        Font companyDetailsFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Paragraph companyDetails = new Paragraph(
                COMPANY_ADDRESS + "\n" +
                        COMPANY_CITY + "\n" +
                        COMPANY_PHONE + " | " + COMPANY_EMAIL + "\n" +
                        COMPANY_GST,
                companyDetailsFont
        );
        companyDetails.setAlignment(Element.ALIGN_CENTER);
        document.add(companyDetails);

        // Add a line separator
        document.add(new Paragraph("\n"));
        addSeparator(document);
    }

    private void addInvoiceInfo(Document document, DateTimeFormatter dateFormatter) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
        Paragraph title = new Paragraph("INVOICE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Paragraph invoiceInfo = new Paragraph(
                "Invoice Date: " + LocalDateTime.now().format(dateFormatter) + "\n" +
                        "Generated On: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")),
                infoFont
        );
        invoiceInfo.setAlignment(Element.ALIGN_RIGHT);
        document.add(invoiceInfo);
    }

    private void addOrderDetailsSection(Document document, Order order, DateTimeFormatter formatter) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Paragraph orderSection = new Paragraph("ORDER DETAILS", sectionFont);
        orderSection.setSpacingBefore(8f);
        orderSection.setSpacingAfter(5f);
        document.add(orderSection);

        // Create a table for order details
        PdfPTable orderDetailsTable = new PdfPTable(2);
        orderDetailsTable.setWidthPercentage(100);
        orderDetailsTable.setWidths(new float[]{1f, 1f});

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        // Left column - Order info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(4);
        leftCell.addElement(new Paragraph("Order ID: " + order.getId(), labelFont));
        leftCell.addElement(new Paragraph("Status: " + order.getOrderStatus(), valueFont));
        leftCell.addElement(new Paragraph("Created: " + order.getCreatedAt().format(formatter), valueFont));
        if (order.getCompletedAt() != null) {
            leftCell.addElement(new Paragraph("Completed: " + order.getCompletedAt().format(formatter), valueFont));
        }

        // Right column - Customer info
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(4);
        rightCell.addElement(new Paragraph("BILL TO:", labelFont));
        rightCell.addElement(new Paragraph(order.getCustomer().getName(), valueFont));

        orderDetailsTable.addCell(leftCell);
        orderDetailsTable.addCell(rightCell);
        orderDetailsTable.setSpacingAfter(5f);
        document.add(orderDetailsTable);
    }

    private void addOrderItemsTable(Document document, Order order) throws DocumentException {
        document.add(new Paragraph(" "));

        // Items header
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        Paragraph itemsHeader = new Paragraph("ORDER ITEMS", sectionFont);
        itemsHeader.setSpacingAfter(8f);
        document.add(itemsHeader);

        // Create items table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 2f, 2f, 1f, 1.5f, 1.5f});

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Color headerColor = new Color(70, 130, 180); // Steel blue

        // Add header cells
        String[] headers = {"Product", "Category", "Brand", "Qty", "Price (₹)", "Total (₹)"};
        for (String header : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(header, headFont));
            hCell.setBackgroundColor(headerColor);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        // Add data rows
        order.getOrderItems().forEach(orderItem -> {
            BigDecimal itemTotal = BigDecimal.valueOf(orderItem.getProduct().getPrice()).multiply(new BigDecimal(orderItem.getQuantity()));

            PdfPCell[] cells = {
                    new PdfPCell(new Phrase(orderItem.getProduct().getName(), cellFont)),
                    new PdfPCell(new Phrase(orderItem.getProduct().getCategory().getName(), cellFont)),
                    new PdfPCell(new Phrase(orderItem.getProduct().getBrandName(), cellFont)),
                    new PdfPCell(new Phrase(orderItem.getQuantity().toString(), cellFont)),
                    new PdfPCell(new Phrase("₹" + orderItem.getProduct().getPrice().toString(), cellFont)),
                    new PdfPCell(new Phrase("₹" + itemTotal.toString(), cellFont))
            };

            for (int i = 0; i < cells.length; i++) {
                cells[i].setPadding(6);
                if (i >= 3) { // Right align quantity, price, and total columns
                    cells[i].setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                table.addCell(cells[i]);
            }
        });

        document.add(table);
    }

    private void addOrderTotal(Document document, Order order) throws DocumentException {
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Paragraph total = new Paragraph("Order Total: ₹" + String.format("%.2f", order.getTotalPrice()), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(5f);
        total.setSpacingAfter(5f);
        document.add(total);
    }

    private void addGrandTotal(Document document, BigDecimal grandTotal) throws DocumentException {
        document.add(new Paragraph("\n"));
        addSeparator(document);

        Font grandTotalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
        Paragraph grandTotalPara = new Paragraph("GRAND TOTAL: ₹" + grandTotal, grandTotalFont);
        grandTotalPara.setAlignment(Element.ALIGN_RIGHT);
        grandTotalPara.setSpacingBefore(10f);
        document.add(grandTotalPara);
    }

    private void addSeparator(Document document) throws DocumentException {
        Paragraph separator = new Paragraph("_".repeat(75));
        separator.setAlignment(Element.ALIGN_CENTER);
        separator.setSpacingBefore(5f);
        separator.setSpacingAfter(5f);
        document.add(separator);
    }

    private void addInvoiceFooter(Document document) throws DocumentException {
        document.add(new Paragraph("\n\n"));

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
        Paragraph footer = new Paragraph(
                "Thank you for your business!\n" +
                        "For any queries, please contact us at " + COMPANY_PHONE + " or " + COMPANY_EMAIL + "\n" +
                        "This is a computer generated invoice.",
                footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }
}
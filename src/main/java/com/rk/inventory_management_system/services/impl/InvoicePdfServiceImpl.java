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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private final OrderService orderService;

    // Company details
    private static final String COMPANY_NAME = "UJJWAL PREM AUTO PARTS AND OIL";
    private static final String COMPANY_ADDRESS = "123 XYZ Complex, Dondaicha";
    private static final String COMPANY_CITY = "Dondaicha, Dhule, Maharashtra 400001";
    private static final String COMPANY_PHONE = "+91-9322505058";
    private static final String COMPANY_EMAIL = "info@ashirwadcompany.com";
//    private static final String COMPANY_GST = "27XXXXX1234X1ZX";
//    private static final String COMPANY_PAN = "ABCDE1234F";
    private static final String COMPANY_WEBSITE = "www.ujjawalpremautoparts.com";

    // Colors
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color SECONDARY_COLOR = new Color(52, 73, 94);
    private static final Color LIGHT_GRAY = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(189, 195, 199);

    @Override
    public ByteArrayInputStream generateInvoice(List<Long> orderIds) {
        List<Order> orders = orderService.getOrdersByIds(orderIds);

        // Reduced margins for more space
        Document document = new Document(PageSize.A4, 25, 25, 25, 25);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // Generate invoice number
            String invoiceNumber = generateInvoiceNumber(orderIds.getFirst());

            // Add header with logo placeholder and company info
            addEnhancedHeader(document, writer);

            // Add invoice title and number
            addInvoiceTitle(document, invoiceNumber);

            // Calculate totals
            BigDecimal subtotal = BigDecimal.ZERO;
            int totalItems = 0;

            for (Order order : orders) {
                subtotal = subtotal.add(BigDecimal.valueOf(order.getTotalPrice()));
                totalItems += order.getOrderItems().size();
            }

            // Add billing and shipping info in columns
            addBillingShippingInfo(document, orders.get(0));

            // Add compact items table
            addCompactItemsTable(document, orders);

            // Add summary section with calculations
            addInvoiceSummary(document, subtotal, orders.get(0));

            // Add payment info and terms
            addPaymentAndTerms(document, orders.get(0));

            // Add footer
            addCompactFooter(document, writer);

            document.close();

        } catch (DocumentException exception) {
            log.error("Error occurred during invoice generation", exception);
            throw new RuntimeException("Error Occurred During Invoice Generation", exception);
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private String generateInvoiceNumber(Long orderId) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("INV-%d%02d%02d-%04d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                (orderId));
    }

    private void addEnhancedHeader(Document document, PdfWriter writer) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3); // Changed to 3 columns
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2f, 1f, 1f}); // Adjusted widths

        // Left side - Company info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPaddingBottom(10);

        // Company name with style
        Font companyFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Paragraph companyName = new Paragraph(COMPANY_NAME, companyFont);
        leftCell.addElement(companyName);

        // Company details
        Font detailsFont = FontFactory.getFont(FontFactory.HELVETICA, 8, SECONDARY_COLOR);
        Paragraph details = new Paragraph();
        details.add(new Phrase(COMPANY_ADDRESS + "\n", detailsFont));
        details.add(new Phrase(COMPANY_CITY + "\n", detailsFont));
        details.add(new Phrase("Phone: " + COMPANY_PHONE + "\n", detailsFont));
        details.add(new Phrase("Email: " + COMPANY_EMAIL + "\n", detailsFont));
        details.add(new Phrase("Website: " + COMPANY_WEBSITE, detailsFont));
        leftCell.addElement(details);

        // Middle - INVOICE heading (centered)
        PdfPCell middleCell = new PdfPCell();
        middleCell.setBorder(Rectangle.NO_BORDER);
        middleCell.setPaddingBottom(10);
        middleCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Font invoiceFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, PRIMARY_COLOR);
        Paragraph invoiceTitle = new Paragraph("INVOICE", invoiceFont);
        invoiceTitle.setAlignment(Element.ALIGN_CENTER);
        middleCell.addElement(invoiceTitle);

        // Right side - Can be used for additional info or logo
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPaddingBottom(10);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        // You can add logo or additional company info here if needed
        Paragraph rightInfo = new Paragraph(" "); // Empty for now
        rightCell.addElement(rightInfo);

        headerTable.addCell(leftCell);
        headerTable.addCell(middleCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);

        // Add a colored line
        drawColoredLine(writer, document, PRIMARY_COLOR, 2f);
    }


    // Update the addInvoiceTitle method to remove the redundant "INVOICE" text from right side
    private void addInvoiceTitle(Document document, String invoiceNumber) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(2);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingBefore(10);
        titleTable.setSpacingAfter(10);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, SECONDARY_COLOR);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        // Invoice number
        PdfPCell invoiceCell = new PdfPCell();
        invoiceCell.setBorder(Rectangle.NO_BORDER);
        Paragraph invoiceP = new Paragraph();
        invoiceP.add(new Phrase("Invoice No: ", titleFont));
        invoiceP.add(new Phrase(invoiceNumber, valueFont));
        invoiceCell.addElement(invoiceP);

        // Date
        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph dateP = new Paragraph();
        dateP.setAlignment(Element.ALIGN_RIGHT);
        dateP.add(new Phrase("Date: ", titleFont));
        dateP.add(new Phrase(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), valueFont));
        dateCell.addElement(dateP);

        titleTable.addCell(invoiceCell);
        titleTable.addCell(dateCell);
        document.add(titleTable);
    }

    private void addBillingShippingInfo(Document document, Order order) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1f, 1f, 1f});
        infoTable.setSpacingAfter(10);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, SECONDARY_COLOR);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

        // Bill To
        PdfPCell billToHeader = new PdfPCell(new Phrase("BILL TO", headerFont));
        billToHeader.setBackgroundColor(PRIMARY_COLOR);
        billToHeader.setPadding(4);
        billToHeader.setBorderColor(PRIMARY_COLOR);

        PdfPCell billToContent = new PdfPCell();
        billToContent.setPadding(5);
        billToContent.setBorderColor(BORDER_COLOR);
        billToContent.setBackgroundColor(LIGHT_GRAY);

        Paragraph billInfo = new Paragraph();
        billInfo.add(new Phrase(order.getCustomer().getName() + "\n", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8)));
        if (order.getCustomer().getAddress() != null) {
            billInfo.add(new Phrase(order.getCustomer().getAddress() + "\n", valueFont));
        }
        billInfo.add(new Phrase("Phone: " + order.getCustomer().getContactNumber() + "\n", valueFont));
        if (order.getCustomer().getEmail() != null) {
            billInfo.add(new Phrase("Email: " + order.getCustomer().getEmail(), valueFont));
        }
        billToContent.addElement(billInfo);

        // Ship To (same as Bill To for now)
        PdfPCell shipToHeader = new PdfPCell(new Phrase("SHIP TO", headerFont));
        shipToHeader.setBackgroundColor(PRIMARY_COLOR);
        shipToHeader.setPadding(4);
        shipToHeader.setBorderColor(PRIMARY_COLOR);

        PdfPCell shipToContent = new PdfPCell();
        shipToContent.setPadding(5);
        shipToContent.setBorderColor(BORDER_COLOR);
        shipToContent.setBackgroundColor(LIGHT_GRAY);
        shipToContent.addElement(billInfo);

        // Order Details
        PdfPCell orderHeader = new PdfPCell(new Phrase("ORDER DETAILS", headerFont));
        orderHeader.setBackgroundColor(PRIMARY_COLOR);
        orderHeader.setPadding(4);
        orderHeader.setBorderColor(PRIMARY_COLOR);

        PdfPCell orderContent = new PdfPCell();
        orderContent.setPadding(5);
        orderContent.setBorderColor(BORDER_COLOR);
        orderContent.setBackgroundColor(LIGHT_GRAY);

        Paragraph orderInfo = new Paragraph();
        orderInfo.add(new Phrase("Order ID: ", labelFont));
        orderInfo.add(new Phrase("#" + order.getId() + "\n", valueFont));
        orderInfo.add(new Phrase("Order Date: ", labelFont));
        orderInfo.add(new Phrase(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + "\n", valueFont));
        orderInfo.add(new Phrase("Status: ", labelFont));
        orderInfo.add(new Phrase(order.getOrderStatus().toString() + "\n", valueFont));
        orderInfo.add(new Phrase("Payment: ", labelFont));
        orderInfo.add(new Phrase(order.getPaymentType().toString(), valueFont));
        orderContent.addElement(orderInfo);

        // Add headers
        infoTable.addCell(billToHeader);
        infoTable.addCell(shipToHeader);
        infoTable.addCell(orderHeader);

        // Add content
        infoTable.addCell(billToContent);
        infoTable.addCell(shipToContent);
        infoTable.addCell(orderContent);

        document.add(infoTable);
    }

    private void addCompactItemsTable(Document document, List<Order> orders) throws DocumentException {
        // Items header with background
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        headerTable.setSpacingBefore(5);

        PdfPCell headerCell = new PdfPCell(new Phrase("ITEM DETAILS",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        headerCell.setBackgroundColor(SECONDARY_COLOR);
        headerCell.setPadding(4);
        headerCell.setBorderColor(SECONDARY_COLOR);
        headerTable.addCell(headerCell);
        document.add(headerTable);

        // Items table
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.5f, 1.5f, 1f, 0.8f, 1f, 1f, 1.2f});

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, SECONDARY_COLOR);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 7);

        // Table headers
        String[] headers = {"#", "Item Description", "Code", "Brand", "Qty", "Rate", "Disc%", "Amount"};
        for (String header : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(header, headFont));
            hCell.setBackgroundColor(LIGHT_GRAY);
            hCell.setPadding(3);
            hCell.setBorderColor(BORDER_COLOR);
            hCell.setHorizontalAlignment(header.equals("Item Description") ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            table.addCell(hCell);
        }

        // Add items
        int itemNo = 1;
        for (Order order : orders) {
            for (var item : order.getOrderItems()) {
                BigDecimal rate = BigDecimal.valueOf(item.getProduct().getSellingPrice());
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
                BigDecimal discount = item.getProduct().getDiscount() != null ?
                        BigDecimal.valueOf(item.getProduct().getDiscount()) : BigDecimal.ZERO;
                BigDecimal amount = rate.multiply(qty).multiply(BigDecimal.ONE.subtract(discount.divide(BigDecimal.valueOf(100))));

                PdfPCell[] cells = {
                        new PdfPCell(new Phrase(String.valueOf(itemNo++), cellFont)),
                        new PdfPCell(new Phrase(item.getProduct().getName() +
                                (item.getProduct().getAttribute() != null ? " - " + item.getProduct().getAttribute() : ""), cellFont)),
                        new PdfPCell(new Phrase(item.getProduct().getProductCode(), cellFont)),
                        new PdfPCell(new Phrase(item.getProduct().getBrandName(), cellFont)),
                        new PdfPCell(new Phrase(qty.toString(), cellFont)),
                        new PdfPCell(new Phrase("₹" + rate.setScale(2, RoundingMode.HALF_UP), cellFont)),
                        new PdfPCell(new Phrase(discount + "%", cellFont)),
                        new PdfPCell(new Phrase("₹" + amount.setScale(2, RoundingMode.HALF_UP), cellFont))
                };

                for (int i = 0; i < cells.length; i++) {
                    cells[i].setPadding(3);
                    cells[i].setBorderColor(BORDER_COLOR);
                    if (i == 1) {
                        cells[i].setHorizontalAlignment(Element.ALIGN_LEFT);
                    } else if (i >= 4) {
                        cells[i].setHorizontalAlignment(Element.ALIGN_RIGHT);
                    } else {
                        cells[i].setHorizontalAlignment(Element.ALIGN_CENTER);
                    }
                    table.addCell(cells[i]);
                }
            }
        }

        document.add(table);
    }

    private void addInvoiceSummary(Document document, BigDecimal subtotal, Order order) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(40);
        summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.setSpacingBefore(10);

        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 7, SECONDARY_COLOR);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 7);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, PRIMARY_COLOR);

        BigDecimal grandTotal = subtotal;

        // Discount if any
        if (order.getOrderItems().stream().anyMatch(i -> i.getProduct().getDiscount() != null && i.getProduct().getDiscount() > 0)) {
            BigDecimal totalDiscount = calculateTotalDiscount(order);
            addSummaryRow(summaryTable, "Total Discount:", "-₹" + totalDiscount.setScale(2, RoundingMode.HALF_UP), labelFont, valueFont);
        }

        // Grand Total with colored background
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("GRAND TOTAL:", totalFont));
        totalLabelCell.setBackgroundColor(LIGHT_GRAY);
        totalLabelCell.setPadding(5);
        totalLabelCell.setBorderColor(PRIMARY_COLOR);
        totalLabelCell.setBorderWidth(1.5f);

        PdfPCell totalValueCell = new PdfPCell(new Phrase("₹" + grandTotal.setScale(2, RoundingMode.HALF_UP), totalFont));
        totalValueCell.setBackgroundColor(LIGHT_GRAY);
        totalValueCell.setPadding(5);
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setBorderColor(PRIMARY_COLOR);
        totalValueCell.setBorderWidth(1.5f);

        summaryTable.addCell(totalLabelCell);
        summaryTable.addCell(totalValueCell);

        document.add(summaryTable);

        // Amount in words - positioned below grand total with proper alignment
        PdfPTable amountWordsTable = new PdfPTable(1);
        amountWordsTable.setWidthPercentage(100);
        amountWordsTable.setSpacingBefore(5);
        amountWordsTable.setSpacingAfter(10);

        Font wordsFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, SECONDARY_COLOR);
        String amountInWords = convertToWords(grandTotal);

        PdfPCell wordsCell = new PdfPCell();
        wordsCell.setBorder(Rectangle.NO_BORDER);
        wordsCell.setPadding(3);

        // Create paragraph with proper text wrapping
        Paragraph amountInWordsPara = new Paragraph();
        amountInWordsPara.add(new Phrase("Amount in words: " + amountInWords, wordsFont));
        amountInWordsPara.setAlignment(Element.ALIGN_LEFT);

        wordsCell.addElement(amountInWordsPara);
        amountWordsTable.addCell(wordsCell);

        document.add(amountWordsTable);
    }

    private void addSummaryRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private BigDecimal calculateTotalDiscount(Order order) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (var item : order.getOrderItems()) {
            if (item.getProduct().getDiscount() != null && item.getProduct().getDiscount() > 0) {
                BigDecimal itemPrice = BigDecimal.valueOf(item.getProduct().getSellingPrice())
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                BigDecimal discount = itemPrice.multiply(BigDecimal.valueOf(item.getProduct().getDiscount()))
                        .divide(BigDecimal.valueOf(100));
                totalDiscount = totalDiscount.add(discount);
            }
        }
        return totalDiscount;
    }

    private void addPaymentAndTerms(Document document, Order order) throws DocumentException {
        PdfPTable termsTable = new PdfPTable(1);
        termsTable.setWidthPercentage(100);
        termsTable.setSpacingBefore(15);

        // Bank Details
//        PdfPCell bankCell = new PdfPCell();
//        bankCell.setBorderColor(BORDER_COLOR);
//        bankCell.setPadding(5);
//        bankCell.setBackgroundColor(LIGHT_GRAY);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, SECONDARY_COLOR);
        Font detailFont = FontFactory.getFont(FontFactory.HELVETICA, 6);

//        Paragraph bankDetails = new Paragraph();
//        bankDetails.add(new Phrase("BANK DETAILS\n", headerFont));
//        bankDetails.add(new Phrase("Bank Name: State Bank of India\n", detailFont));
//        bankDetails.add(new Phrase("Account No: 1234567890\n", detailFont));
//        bankDetails.add(new Phrase("IFSC Code: SBIN0001234\n", detailFont));
//        bankDetails.add(new Phrase("Branch: Dondaicha", detailFont));
//        bankCell.addElement(bankDetails);

        // Terms & Conditions
        PdfPCell termsCell = new PdfPCell();
        termsCell.setBorderColor(BORDER_COLOR);
        termsCell.setPadding(5);
        termsCell.setBackgroundColor(LIGHT_GRAY);

        Paragraph terms = new Paragraph();
        terms.add(new Phrase("TERMS & CONDITIONS\n", headerFont));
        terms.add(new Phrase("• Goods once sold will not be taken back\n", detailFont));
//        terms.add(new Phrase("• Interest @18% p.a. will be charged on delayed payments\n", detailFont));
//        terms.add(new Phrase("• Subject to local jurisdiction\n", detailFont));
        terms.add(new Phrase("• E. & O.E.", detailFont));
        termsCell.addElement(terms);

//        termsTable.addCell(bankCell);
        termsTable.addCell(termsCell);
        document.add(termsTable);
    }

    private void addCompactFooter(Document document, PdfWriter writer) throws DocumentException {
        // Signature section
        PdfPTable signTable = new PdfPTable(2);
        signTable.setWidthPercentage(100);
        signTable.setSpacingBefore(20);

        Font signFont = FontFactory.getFont(FontFactory.HELVETICA, 7, SECONDARY_COLOR);

        PdfPCell receiverSign = new PdfPCell();
        receiverSign.setBorder(Rectangle.TOP);
        receiverSign.setBorderColor(BORDER_COLOR);
        receiverSign.setPaddingTop(5);
        Paragraph receiverP = new Paragraph("Receiver's Signature", signFont);
        receiverP.setAlignment(Element.ALIGN_CENTER);
        receiverSign.addElement(receiverP);

        PdfPCell authSign = new PdfPCell();
        authSign.setBorder(Rectangle.TOP);
        authSign.setBorderColor(BORDER_COLOR);
        authSign.setPaddingTop(5);
        Paragraph authP = new Paragraph("Authorized Signature", signFont);
        authP.setAlignment(Element.ALIGN_CENTER);
        authSign.addElement(authP);

        signTable.addCell(receiverSign);
        signTable.addCell(authSign);
        document.add(signTable);

        // Footer text
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 6, Color.GRAY);
        Paragraph footer = new Paragraph();
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(10);
        footer.add(new Phrase("This is a computer generated invoice and does not require physical signature\n", footerFont));
        footer.add(new Phrase("Thank you for your business!", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, PRIMARY_COLOR)));
        document.add(footer);
    }

    private void drawColoredLine(PdfWriter writer, Document document, Color color, float thickness) {
        PdfContentByte canvas = writer.getDirectContent();
        canvas.setColorStroke(color);
        canvas.setLineWidth(thickness);
        canvas.moveTo(document.left(), writer.getVerticalPosition(false));
        canvas.lineTo(document.right(), writer.getVerticalPosition(false));
        canvas.stroke();
    }

    private String convertToWords(BigDecimal amount) {
        // Simple implementation - you can use a library for better conversion
        long number = amount.longValue();
        if (number == 0) return "Zero Rupees Only";

        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

        // Simple conversion for demonstration - enhance as needed
        return "Rupees " + String.format("%.0f", amount.doubleValue()) + " Only";
    }
}
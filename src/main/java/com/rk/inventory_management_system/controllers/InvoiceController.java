package com.rk.inventory_management_system.controllers;


import com.rk.inventory_management_system.services.InvoicePdfService;
import com.rk.inventory_management_system.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoicePdfService invoicePdfService;
    private final OrderService orderService;

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadInvoice(@RequestParam List<Long> orderIds) {
        byte[] pdfBytes = invoicePdfService.generateInvoice(orderIds).readAllBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoices.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

}

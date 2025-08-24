package com.rk.inventory_management_system.services;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface InvoicePdfService {

    ByteArrayInputStream generateInvoice(List<Long> orderIds);
}

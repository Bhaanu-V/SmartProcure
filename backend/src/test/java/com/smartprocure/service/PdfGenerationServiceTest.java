package com.smartprocure.service;

import com.smartprocure.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PdfGenerationServiceTest {

    private final PdfGenerationService pdfService = new PdfGenerationService();

    @Test
    @DisplayName("PDF Generation: Successfully generates valid PDF byte stream with %PDF magic header")
    void testGeneratePurchaseOrderPdf_Success() {
        Department dept = new Department("FIN", "Finance Dept", "Accounting and Budget");
        User requester = new User("sarah", "sarah@smartprocure.io", "hash", "Sarah Requester", Role.EMPLOYEE);
        Supplier supplier = new Supplier("Global Logistics Corp", "sales@globallogistics.com", "NET_30");
        supplier.setContactPhone("+1-555-0199");
        supplier.setAddress("742 Evergreen Terrace, Sector 4");

        PurchaseRequest pr = new PurchaseRequest("PR-20261014-9999", "Office Supplies Q4", requester, dept);
        pr.setId(42L);

        PurchaseOrder po = new PurchaseOrder("PO-20261014-8888", "Office Supplies Q4", supplier.getName(), new BigDecimal("1250.00"));
        po.setId(99L);
        po.setDepartment(dept);
        po.setRequester(requester);
        po.setSupplier(supplier);
        po.setPurchaseRequest(pr);
        po.setStatus(POStatus.ISSUED);
        po.setPriority(Priority.HIGH);
        po.setCurrency("USD");

        PurchaseOrderItem item1 = new PurchaseOrderItem("High-Yield Toner Cartridges", new BigDecimal("5.00"), new BigDecimal("150.00"));
        item1.setCategory("OFFICE_SUPPLIES");
        po.addLineItem(item1);

        PurchaseOrderItem item2 = new PurchaseOrderItem("Ergonomic Mesh Chair", new BigDecimal("2.00"), new BigDecimal("250.00"));
        item2.setCategory("FURNITURE");
        po.addLineItem(item2);

        byte[] pdfBytes = pdfService.generatePurchaseOrderPdf(po);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF document size should be reasonably large");

        // Verify PDF Magic Number Header: %PDF (0x25 0x50 0x44 0x46)
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}

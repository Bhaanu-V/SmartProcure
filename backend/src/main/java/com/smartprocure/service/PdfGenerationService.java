package com.smartprocure.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartprocure.entity.PurchaseOrder;
import com.smartprocure.entity.PurchaseOrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating digitally formatted enterprise Purchase Order PDF documents.
 */
@Service
public class PdfGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Color PRIMARY_COLOR = new Color(30, 58, 138);     // #1E3A8A Navy
    private static final Color TEXT_COLOR = new Color(30, 41, 59);          // #1E293B Dark Slate
    private static final Color MUTED_COLOR = new Color(100, 116, 139);      // #64748B Slate Gray
    private static final Color LIGHT_BG = new Color(248, 250, 252);        // #F8FAFC
    private static final Color HEADER_BG = new Color(238, 242, 255);       // #EEF2FF
    private static final Color BORDER_COLOR = new Color(203, 213, 225);     // #CBD5E1

    public byte[] generatePurchaseOrderPdf(PurchaseOrder po) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, po);
            addOrderMetadata(document, po);
            addLineItemsTable(document, po);
            addCertificationFooter(document, po);

            document.close();
            log.info("Successfully generated PDF for Purchase Order '{}' ({} bytes)", po.getPoNumber(), out.size());
            return out.toByteArray();
        } catch (Exception ex) {
            log.error("Failed to generate PDF for purchase order '{}': {}", po.getPoNumber(), ex.getMessage(), ex);
            throw new RuntimeException("Error generating Purchase Order PDF document: " + ex.getMessage(), ex);
        }
    }

    private void addHeader(Document document, PurchaseOrder po) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        // Left: Branding
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);

        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, PRIMARY_COLOR);
        Paragraph brand = new Paragraph("SmartProcure Enterprise", brandFont);
        brandCell.addElement(brand);

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED_COLOR);
        Paragraph subtitle = new Paragraph("Automated Procurement & Approval Management System", subFont);
        brandCell.addElement(subtitle);

        headerTable.addCell(brandCell);

        // Right: Title & PO Number
        PdfPCell poCell = new PdfPCell();
        poCell.setBorder(Rectangle.NO_BORDER);
        poCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXT_COLOR);
        Paragraph title = new Paragraph("PURCHASE ORDER", titleFont);
        title.setAlignment(Element.ALIGN_RIGHT);
        poCell.addElement(title);

        Font poNumFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR);
        Paragraph poNum = new Paragraph("PO #: " + po.getPoNumber(), poNumFont);
        poNum.setAlignment(Element.ALIGN_RIGHT);
        poCell.addElement(poNum);

        headerTable.addCell(poCell);
        document.add(headerTable);

        // Spacer
        document.add(new Paragraph(" "));
    }

    private void addOrderMetadata(Document document, PurchaseOrder po) throws DocumentException {
        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setWidths(new float[]{50, 50});

        // Vendor Information Box
        PdfPCell vendorCell = new PdfPCell();
        vendorCell.setBackgroundColor(LIGHT_BG);
        vendorCell.setBorderColor(BORDER_COLOR);
        vendorCell.setPadding(10);

        Font sectionHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR);
        vendorCell.addElement(new Paragraph("VENDOR / SUPPLIER DETAILS", sectionHeaderFont));

        Font boldText = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT_COLOR);
        Font regularText = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_COLOR);

        vendorCell.addElement(new Paragraph(po.getVendorName(), boldText));
        if (po.getSupplier() != null) {
            if (po.getSupplier().getContactEmail() != null) {
                vendorCell.addElement(new Paragraph("Email: " + po.getSupplier().getContactEmail(), regularText));
            }
            if (po.getSupplier().getContactPhone() != null) {
                vendorCell.addElement(new Paragraph("Phone: " + po.getSupplier().getContactPhone(), regularText));
            }
            if (po.getSupplier().getAddress() != null) {
                vendorCell.addElement(new Paragraph("Address: " + po.getSupplier().getAddress(), regularText));
            }
        }
        metaTable.addCell(vendorCell);

        // Department & Order Details Box
        PdfPCell deptCell = new PdfPCell();
        deptCell.setBackgroundColor(LIGHT_BG);
        deptCell.setBorderColor(BORDER_COLOR);
        deptCell.setPadding(10);

        deptCell.addElement(new Paragraph("ORDER & DEPARTMENT INFO", sectionHeaderFont));
        if (po.getDepartment() != null) {
            deptCell.addElement(new Paragraph("Department: " + po.getDepartment().getName() + " (" + po.getDepartment().getCode() + ")", regularText));
        }
        if (po.getRequester() != null) {
            deptCell.addElement(new Paragraph("Requester: " + po.getRequester().getFullName() + " (" + po.getRequester().getEmail() + ")", regularText));
        }
        if (po.getPurchaseRequest() != null) {
            deptCell.addElement(new Paragraph("Requisition Ref: " + po.getPurchaseRequest().getRequestNumber(), regularText));
        }
        deptCell.addElement(new Paragraph("Status: " + po.getStatus().name() + " | Priority: " + po.getPriority().name(), regularText));
        if (po.getCreatedAt() != null) {
            deptCell.addElement(new Paragraph("Issued Date: " + po.getCreatedAt().format(DATE_FORMATTER), regularText));
        }

        metaTable.addCell(deptCell);
        document.add(metaTable);

        document.add(new Paragraph(" "));
    }

    private void addLineItemsTable(Document document, PurchaseOrder po) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{6, 38, 16, 10, 15, 15});

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR);

        addTableHeaderCell(table, "#", headerFont);
        addTableHeaderCell(table, "Item Description", headerFont);
        addTableHeaderCell(table, "Category", headerFont);
        addTableHeaderCell(table, "Qty", headerFont);
        addTableHeaderCell(table, "Unit Price", headerFont);
        addTableHeaderCell(table, "Total Price", headerFont);

        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_COLOR);

        int index = 1;
        if (po.getLineItems() != null) {
            for (PurchaseOrderItem item : po.getLineItems()) {
                Color rowBg = (index % 2 == 0) ? LIGHT_BG : Color.WHITE;

                addTableCell(table, String.valueOf(index), bodyFont, Element.ALIGN_CENTER, rowBg);
                addTableCell(table, item.getItemDescription(), bodyFont, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, item.getCategory() != null ? item.getCategory() : "-", bodyFont, Element.ALIGN_LEFT, rowBg);
                addTableCell(table, item.getQuantity().stripTrailingZeros().toPlainString(), bodyFont, Element.ALIGN_RIGHT, rowBg);
                addTableCell(table, String.format("$%.2f", item.getUnitPrice()), bodyFont, Element.ALIGN_RIGHT, rowBg);
                addTableCell(table, String.format("$%.2f", item.getTotalPrice()), bodyFont, Element.ALIGN_RIGHT, rowBg);

                index++;
            }
        }

        // Summary Row: Grand Total
        PdfPCell totalLabelCell = new PdfPCell(new Phrase("GRAND TOTAL (" + po.getCurrency() + "):", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR)));
        totalLabelCell.setColspan(5);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setBackgroundColor(HEADER_BG);
        totalLabelCell.setPadding(8);
        table.addCell(totalLabelCell);

        BigDecimal total = po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO;
        PdfPCell totalValCell = new PdfPCell(new Phrase(String.format("$%.2f", total), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, PRIMARY_COLOR)));
        totalValCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValCell.setBackgroundColor(HEADER_BG);
        totalValCell.setPadding(8);
        table.addCell(totalValCell);

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addCertificationFooter(Document document, PurchaseOrder po) throws DocumentException {
        PdfPTable footerTable = new PdfPTable(1);
        footerTable.setWidthPercentage(100);

        PdfPCell footerCell = new PdfPCell();
        footerCell.setBackgroundColor(LIGHT_BG);
        footerCell.setBorderColor(BORDER_COLOR);
        footerCell.setPadding(8);

        Font certFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PRIMARY_COLOR);
        footerCell.addElement(new Paragraph("OFFICIAL DIGITAL CERTIFICATION", certFont));

        Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED_COLOR);
        footerCell.addElement(new Paragraph(
                "This document has been electronically issued and verified by SmartProcure Workflow Engine. " +
                "All budget reservations and approvals (Manager & Finance) have been cryptographically verified. " +
                "This Purchase Order constitutes a legally binding enterprise order upon transmission.",
                descFont
        ));

        footerTable.addCell(footerCell);
        document.add(footerTable);
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(HEADER_BG);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER_COLOR);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }
}

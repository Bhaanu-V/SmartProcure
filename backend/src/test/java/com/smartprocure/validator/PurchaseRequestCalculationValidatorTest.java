package com.smartprocure.validator;

import com.smartprocure.dto.pr.CreateLineItemRequest;
import com.smartprocure.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PurchaseRequestCalculationValidatorTest {

    private PurchaseRequestCalculationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PurchaseRequestCalculationValidator();
    }

    @Test
    @DisplayName("Calculates line total correctly with half-up rounding")
    void testValidateAndCalculateLineTotal_Success() {
        CreateLineItemRequest item = new CreateLineItemRequest("Mechanical Keyboards", new BigDecimal("5.00"), new BigDecimal("120.50"));
        BigDecimal lineTotal = validator.validateAndCalculateLineTotal(item);

        assertEquals(0, lineTotal.compareTo(new BigDecimal("602.50")));
    }

    @Test
    @DisplayName("Calculates grand total for multiple line items accurately")
    void testCalculateGrandTotal_Success() {
        CreateLineItemRequest item1 = new CreateLineItemRequest("Monitor 27-inch", new BigDecimal("2.00"), new BigDecimal("299.99"));
        CreateLineItemRequest item2 = new CreateLineItemRequest("USB-C Hub", new BigDecimal("4.00"), new BigDecimal("49.50"));

        BigDecimal total = validator.calculateGrandTotal(Arrays.asList(item1, item2));
        // (2 * 299.99) + (4 * 49.50) = 599.98 + 198.00 = 797.98
        assertEquals(0, total.compareTo(new BigDecimal("797.98")));
    }

    @Test
    @DisplayName("Rejects negative or zero quantity")
    void testInvalidQuantity() {
        CreateLineItemRequest zeroQty = new CreateLineItemRequest("Mouse", BigDecimal.ZERO, new BigDecimal("25.00"));
        CreateLineItemRequest negQty = new CreateLineItemRequest("Mouse", new BigDecimal("-2.00"), new BigDecimal("25.00"));

        assertThrows(BadRequestException.class, () -> validator.validateAndCalculateLineTotal(zeroQty));
        assertThrows(BadRequestException.class, () -> validator.validateAndCalculateLineTotal(negQty));
    }

    @Test
    @DisplayName("Rejects negative or zero unit price")
    void testInvalidUnitPrice() {
        CreateLineItemRequest zeroPrice = new CreateLineItemRequest("Cable", new BigDecimal("1.00"), BigDecimal.ZERO);
        CreateLineItemRequest negPrice = new CreateLineItemRequest("Cable", new BigDecimal("1.00"), new BigDecimal("-5.00"));

        assertThrows(BadRequestException.class, () -> validator.validateAndCalculateLineTotal(zeroPrice));
        assertThrows(BadRequestException.class, () -> validator.validateAndCalculateLineTotal(negPrice));
    }

    @Test
    @DisplayName("Rejects empty or null line items list")
    void testEmptyItemsList() {
        assertThrows(BadRequestException.class, () -> validator.calculateGrandTotal(Collections.emptyList()));
        assertThrows(BadRequestException.class, () -> validator.calculateGrandTotal(null));
    }
}

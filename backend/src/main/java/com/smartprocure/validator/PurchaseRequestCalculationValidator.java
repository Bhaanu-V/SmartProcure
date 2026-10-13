package com.smartprocure.validator;

import com.smartprocure.dto.pr.CreateLineItemRequest;
import com.smartprocure.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Server-side calculation and boundary validation component for purchase requests and line items.
 * Guarantees arithmetic integrity, prevents negative/zero amounts, and enforces server-computed totals.
 */
@Component
public class PurchaseRequestCalculationValidator {

    private static final BigDecimal MAX_QUANTITY = new BigDecimal("1000000.00");
    private static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("100000000.00");

    /**
     * Validates an individual line item's quantity and price and returns the server-calculated line total.
     */
    public BigDecimal validateAndCalculateLineTotal(CreateLineItemRequest item) {
        if (item == null) {
            throw new BadRequestException("Line item cannot be null");
        }

        BigDecimal qty = item.getQuantity();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Line item quantity must be strictly greater than zero");
        }
        if (qty.compareTo(MAX_QUANTITY) > 0) {
            throw new BadRequestException("Line item quantity exceeds maximum permissible limit: " + MAX_QUANTITY);
        }

        BigDecimal unitPrice = item.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Line item unit price must be strictly greater than zero");
        }
        if (unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
            throw new BadRequestException("Line item unit price exceeds maximum permissible limit: " + MAX_UNIT_PRICE);
        }

        return qty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the grand total for a list of line items, validating every item on the server.
     */
    public BigDecimal calculateGrandTotal(List<CreateLineItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("Purchase request must contain at least one line item");
        }

        BigDecimal grandTotal = BigDecimal.ZERO;
        for (CreateLineItemRequest item : items) {
            BigDecimal lineTotal = validateAndCalculateLineTotal(item);
            grandTotal = grandTotal.add(lineTotal);
        }

        return grandTotal.setScale(2, RoundingMode.HALF_UP);
    }
}

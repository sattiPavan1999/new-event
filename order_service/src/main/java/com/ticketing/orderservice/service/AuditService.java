package com.ticketing.orderservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    public void logOrderCreated(Long orderId, Long buyerId, Integer itemCount, String totalAmount) {
        auditLogger.info("ORDER_CREATED - orderId={}, buyerId={}, itemCount={}, totalAmount={}",
                orderId, buyerId, itemCount, maskAmount(totalAmount));
    }

    public void logOrderConfirmed(Long orderId) {
        auditLogger.info("ORDER_CONFIRMED - orderId={}", orderId);
    }

    public void logOrderCancelled(Long orderId) {
        auditLogger.info("ORDER_CANCELLED - orderId={}", orderId);
    }

    public void logOrderFailed(Long orderId, String reason) {
        auditLogger.info("ORDER_FAILED - orderId={}, reason={}", orderId, reason);
    }

    public void logInventoryDecremented(Long tierId, Integer quantity) {
        auditLogger.info("INVENTORY_DECREMENTED - tierId={}, quantity={}", tierId, quantity);
    }

    public void logError(String errorCode, String message) {
        auditLogger.error("ERROR - errorCode={}, message={}", errorCode, message);
    }

    private String maskAmount(String amount) {
        return "***";
    }
}

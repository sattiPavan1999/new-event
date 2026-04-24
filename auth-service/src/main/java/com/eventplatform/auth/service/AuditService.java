package com.eventplatform.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    public void logRegistration(String email, String role, boolean success) {
        if (success) {
            auditLogger.info("User registration successful - Email: {}, Role: {}", maskEmail(email), role);
        } else {
            auditLogger.warn("User registration failed - Email: {}, Role: {}", maskEmail(email), role);
        }
    }

    public void logLogin(String email, boolean success) {
        if (success) {
            auditLogger.info("User login successful - Email: {}", maskEmail(email));
        } else {
            auditLogger.warn("User login failed - Email: {}", maskEmail(email));
        }
    }

    public void logTokenRefresh(String userId, boolean success) {
        if (success) {
            auditLogger.info("Token refresh successful - UserID: {}", maskId(userId));
        } else {
            auditLogger.warn("Token refresh failed - UserID: {}", maskId(userId));
        }
    }

    public void logLogout(String userId, boolean success) {
        if (success) {
            auditLogger.info("User logout successful - UserID: {}", maskId(userId));
        } else {
            auditLogger.warn("User logout failed - UserID: {}", maskId(userId));
        }
    }

    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***" + email.substring(atIndex);
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    private String maskId(String id) {
        if (id == null || id.length() < 8) {
            return "***";
        }
        return id.substring(0, 4) + "***" + id.substring(id.length() - 4);
    }
}

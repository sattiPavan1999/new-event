package com.eventmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    public void logEventCreated(UUID eventId, UUID organiserId, String title) {
        logger.info("Event created: eventId={}, organiserId={}, title=***", eventId, organiserId);
    }

    public void logEventUpdated(UUID eventId, UUID organiserId, String title) {
        logger.info("Event updated: eventId={}, organiserId={}, title=***", eventId, organiserId);
    }

    public void logEventPublished(UUID eventId, UUID organiserId) {
        logger.info("Event published: eventId={}, organiserId={}", eventId, organiserId);
    }

    public void logEventCancelled(UUID eventId, UUID organiserId) {
        logger.info("Event cancelled: eventId={}, organiserId={}", eventId, organiserId);
    }

    public void logTierCreated(UUID tierId, UUID eventId, String tierName) {
        logger.info("Tier created: tierId={}, eventId={}, tierName=***", tierId, eventId);
    }

    public void logTierUpdated(UUID tierId, UUID eventId, String tierName) {
        logger.info("Tier updated: tierId={}, eventId={}, tierName=***", tierId, eventId);
    }

    public void logTierDeleted(UUID tierId, UUID eventId) {
        logger.info("Tier deleted: tierId={}, eventId={}", tierId, eventId);
    }

    public void logEventBrowsed(String category, String city, String search, int page) {
        logger.debug("Events browsed: category={}, city={}, search=***, page={}", category, city, page);
    }

    public void logEventDetailViewed(UUID eventId) {
        logger.debug("Event detail viewed: eventId={}", eventId);
    }

    public void logSalesSummaryViewed(UUID eventId, UUID organiserId) {
        logger.info("Sales summary viewed: eventId={}, organiserId={}", eventId, organiserId);
    }
}

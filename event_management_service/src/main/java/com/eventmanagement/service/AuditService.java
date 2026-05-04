package com.eventmanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    public void logEventCreated(Long eventId, Long organiserId, String title) {
        logger.info("Event created: eventId={}, organiserId={}, title=***", eventId, organiserId);
    }

    public void logEventUpdated(Long eventId, Long organiserId, String title) {
        logger.info("Event updated: eventId={}, organiserId={}, title=***", eventId, organiserId);
    }

    public void logEventPublished(Long eventId, Long organiserId) {
        logger.info("Event published: eventId={}, organiserId={}", eventId, organiserId);
    }

    public void logEventCancelled(Long eventId, Long organiserId) {
        logger.info("Event cancelled: eventId={}, organiserId={}", eventId, organiserId);
    }

    public void logTierCreated(Long tierId, Long eventId, String tierName) {
        logger.info("Tier created: tierId={}, eventId={}, tierName=***", tierId, eventId);
    }

    public void logTierUpdated(Long tierId, Long eventId, String tierName) {
        logger.info("Tier updated: tierId={}, eventId={}, tierName=***", tierId, eventId);
    }

    public void logTierDeleted(Long tierId, Long eventId) {
        logger.info("Tier deleted: tierId={}, eventId={}", tierId, eventId);
    }

    public void logEventBrowsed(String category, String city, String search, int page) {
        logger.debug("Events browsed: category={}, city={}, search=***, page={}", category, city, page);
    }

    public void logEventDetailViewed(Long eventId) {
        logger.debug("Event detail viewed: eventId={}", eventId);
    }

    public void logSalesSummaryViewed(Long eventId, Long organiserId) {
        logger.info("Sales summary viewed: eventId={}, organiserId={}", eventId, organiserId);
    }
}

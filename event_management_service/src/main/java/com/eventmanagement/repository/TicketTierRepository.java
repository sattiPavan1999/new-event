package com.eventmanagement.repository;

import com.eventmanagement.entity.TicketTier;
import com.eventmanagement.enums.TierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, UUID> {

    @Query("SELECT COUNT(t) FROM TicketTier t WHERE t.event.id = :eventId AND t.status = :status")
    long countByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") TierStatus status);

    @Query("SELECT COUNT(t) FROM TicketTier t WHERE t.event.id = :eventId")
    long countByEventId(@Param("eventId") UUID eventId);

    List<TicketTier> findByEventId(UUID eventId);

    List<TicketTier> findByEventIdIn(List<UUID> eventIds);

    @Query("SELECT t.event.id, MIN(t.price) FROM TicketTier t WHERE t.event.id IN :eventIds AND t.status = :status GROUP BY t.event.id")
    List<Object[]> findMinActivePricesByEventIds(@Param("eventIds") List<UUID> eventIds, @Param("status") TierStatus status);
}

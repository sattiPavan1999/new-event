package com.eventmanagement.repository;

import com.eventmanagement.entity.TicketTier;
import com.eventmanagement.enums.TierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketTierRepository extends JpaRepository<TicketTier, Long> {

    @Query("SELECT COUNT(t) FROM TicketTier t WHERE t.event.id = :eventId AND t.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") TierStatus status);

    @Query("SELECT COUNT(t) FROM TicketTier t WHERE t.event.id = :eventId")
    long countByEventId(@Param("eventId") Long eventId);

    List<TicketTier> findByEventId(Long eventId);

    List<TicketTier> findByEventIdIn(List<Long> eventIds);

    @Query("SELECT t.event.id, MIN(t.price) FROM TicketTier t WHERE t.event.id IN :eventIds AND t.status = :status GROUP BY t.event.id")
    List<Object[]> findMinActivePricesByEventIds(@Param("eventIds") List<Long> eventIds, @Param("status") TierStatus status);
}

package com.eventmanagement.repository;

import com.eventmanagement.entity.Event;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(value = "SELECT e FROM Event e LEFT JOIN FETCH e.venue WHERE e.organiserId = :organiserId ORDER BY e.createdAt DESC",
           countQuery = "SELECT COUNT(e) FROM Event e WHERE e.organiserId = :organiserId")
    Page<Event> findByOrganiserIdOrderByCreatedAtDesc(@Param("organiserId") Long organiserId, Pageable pageable);

    @Query(value = "SELECT e FROM Event e LEFT JOIN FETCH e.venue v WHERE e.status = :status AND e.eventDate > :now " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:city IS NULL OR LOWER(v.city) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%'))) " +
            "AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
            "ORDER BY e.eventDate ASC",
           countQuery = "SELECT COUNT(e) FROM Event e WHERE e.status = :status AND e.eventDate > :now " +
            "AND (:category IS NULL OR e.category = :category) " +
            "AND (:city IS NULL OR LOWER(e.venue.city) LIKE LOWER(CONCAT('%', CAST(:city AS string), '%'))) " +
            "AND (:search IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) " +
            "OR LOWER(e.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Event> findPublishedEvents(
            @Param("status") EventStatus status,
            @Param("now") LocalDateTime now,
            @Param("category") EventCategory category,
            @Param("city") String city,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.venue LEFT JOIN FETCH e.ticketTiers WHERE e.id = :eventId")
    Optional<Event> findWithDetailsById(@Param("eventId") Long eventId);
}

package com.eventmanagement.service;

import com.eventmanagement.dto.CreateEventRequest;
import com.eventmanagement.dto.CreateTierRequest;
import com.eventmanagement.dto.EventDetailResponse;
import com.eventmanagement.dto.EventResponse;
import com.eventmanagement.dto.EventSummaryResponse;
import com.eventmanagement.dto.PageResponse;
import com.eventmanagement.dto.SalesSummaryResponse;
import com.eventmanagement.dto.TierResponse;
import com.eventmanagement.dto.VenueDto;
import com.eventmanagement.entity.Event;
import com.eventmanagement.entity.TicketTier;
import com.eventmanagement.entity.Venue;
import com.eventmanagement.enums.EventCategory;
import com.eventmanagement.enums.EventStatus;
import com.eventmanagement.enums.TierStatus;
import com.eventmanagement.exception.BusinessRuleViolationException;
import com.eventmanagement.exception.ResourceNotFoundException;
import com.eventmanagement.repository.EventRepository;
import com.eventmanagement.repository.TicketTierRepository;
import com.eventmanagement.repository.VenueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final TicketTierRepository ticketTierRepository;
    private final AuditService auditService;

    public EventService(EventRepository eventRepository, VenueRepository venueRepository,
                        TicketTierRepository ticketTierRepository, AuditService auditService) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.ticketTierRepository = ticketTierRepository;
        this.auditService = auditService;
    }

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, UUID organiserId) {
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.getVenueId()));

        UUID eventId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        Event event = new Event(
                eventId,
                organiserId,
                venue,
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                request.getEventDate(),
                request.getBannerImageUrl(),
                EventStatus.DRAFT,
                now,
                now
        );

        Event savedEvent = eventRepository.save(event);
        auditService.logEventCreated(eventId, organiserId, request.getTitle());

        return toEventResponse(savedEvent);
    }

    @Transactional
    public TierResponse addTier(UUID eventId, CreateTierRequest request, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        long tierCount = ticketTierRepository.countByEventId(eventId);
        if (tierCount >= 10) {
            throw new BusinessRuleViolationException("Maximum 10 tiers allowed per event");
        }

        if (request.getSaleStartsAt() != null && request.getSaleEndsAt() != null) {
            if (request.getSaleEndsAt().isBefore(request.getSaleStartsAt())) {
                throw new BusinessRuleViolationException("Sale end time must be after sale start time");
            }
        }

        UUID tierId = UUID.randomUUID();
        Integer maxPerOrder = request.getMaxPerOrder() != null ? request.getMaxPerOrder() : 10;

        TicketTier tier = new TicketTier(
                tierId,
                event,
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getTotalQty(),
                request.getTotalQty(),
                maxPerOrder,
                request.getSaleStartsAt(),
                request.getSaleEndsAt(),
                TierStatus.ACTIVE,
                LocalDateTime.now()
        );

        TicketTier savedTier = ticketTierRepository.save(tier);
        auditService.logTierCreated(tierId, eventId, request.getName());

        return toTierResponse(savedTier);
    }

    @Transactional
    public TierResponse updateTier(UUID eventId, UUID tierId, CreateTierRequest request, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        TicketTier tier = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found with id: " + tierId));

        if (!tier.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleViolationException("Tier does not belong to this event");
        }

        boolean hasConfirmedOrders = tier.getRemainingQty() < tier.getTotalQty();

        if (hasConfirmedOrders) {
            if (!request.getPrice().equals(tier.getPrice())) {
                throw new BusinessRuleViolationException("Cannot modify price - confirmed orders exist for this tier");
            }
            if (!request.getTotalQty().equals(tier.getTotalQty())) {
                throw new BusinessRuleViolationException("Cannot modify quantity - confirmed orders exist for this tier");
            }
        }

        if (request.getSaleStartsAt() != null && request.getSaleEndsAt() != null) {
            if (request.getSaleEndsAt().isBefore(request.getSaleStartsAt())) {
                throw new BusinessRuleViolationException("Sale end time must be after sale start time");
            }
        }

        tier.setName(request.getName());
        tier.setDescription(request.getDescription());
        if (!hasConfirmedOrders) {
            tier.setPrice(request.getPrice());
            int qtySold = tier.getTotalQty() - tier.getRemainingQty();
            tier.setTotalQty(request.getTotalQty());
            tier.setRemainingQty(request.getTotalQty() - qtySold);
        }
        if (request.getMaxPerOrder() != null) {
            tier.setMaxPerOrder(request.getMaxPerOrder());
        }
        tier.setSaleStartsAt(request.getSaleStartsAt());
        tier.setSaleEndsAt(request.getSaleEndsAt());

        TicketTier updatedTier = ticketTierRepository.save(tier);
        auditService.logTierUpdated(tierId, eventId, request.getName());

        return toTierResponse(updatedTier);
    }

    @Transactional
    public void deleteTier(UUID eventId, UUID tierId, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        TicketTier tier = ticketTierRepository.findById(tierId)
                .orElseThrow(() -> new ResourceNotFoundException("Tier not found with id: " + tierId));

        if (!tier.getEvent().getId().equals(eventId)) {
            throw new BusinessRuleViolationException("Tier does not belong to this event");
        }

        boolean hasOrders = tier.getRemainingQty() < tier.getTotalQty();
        if (hasOrders) {
            throw new BusinessRuleViolationException("Cannot delete tier - orders exist for this tier");
        }

        ticketTierRepository.delete(tier);
        auditService.logTierDeleted(tierId, eventId);
    }

    @Transactional
    public EventResponse updateEvent(UUID eventId, CreateEventRequest request, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        if (event.getStatus() != EventStatus.DRAFT) {
            if (!request.getEventDate().equals(event.getEventDate())) {
                throw new BusinessRuleViolationException("Event date can only be edited in DRAFT status");
            }
            if (!request.getVenueId().equals(event.getVenue().getId())) {
                throw new BusinessRuleViolationException("Venue can only be edited in DRAFT status");
            }
        }

        if (!request.getVenueId().equals(event.getVenue().getId())) {
            Venue newVenue = venueRepository.findById(request.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue not found with id: " + request.getVenueId()));
            event.setVenue(newVenue);
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setEventDate(request.getEventDate());
        event.setBannerImageUrl(request.getBannerImageUrl());
        event.setUpdatedAt(LocalDateTime.now());

        Event updatedEvent = eventRepository.save(event);
        auditService.logEventUpdated(eventId, organiserId, request.getTitle());

        return toEventResponse(updatedEvent);
    }

    @Transactional
    public EventResponse publishEvent(UUID eventId, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT events can be published");
        }

        long activeTierCount = ticketTierRepository.countByEventIdAndStatus(eventId, TierStatus.ACTIVE);
        if (activeTierCount == 0) {
            throw new BusinessRuleViolationException("Event must have at least one ACTIVE tier before publishing");
        }

        event.setStatus(EventStatus.PUBLISHED);
        event.setUpdatedAt(LocalDateTime.now());

        Event publishedEvent = eventRepository.save(event);
        auditService.logEventPublished(eventId, organiserId);

        return toEventResponse(publishedEvent);
    }

    @Transactional
    public EventResponse cancelEvent(UUID eventId, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        event.setStatus(EventStatus.CANCELLED);
        event.setUpdatedAt(LocalDateTime.now());

        Event cancelledEvent = eventRepository.save(event);
        auditService.logEventCancelled(eventId, organiserId);

        return toEventResponse(cancelledEvent);
    }

    @Transactional(readOnly = true)
    public PageResponse<EventSummaryResponse> browseEvents(EventCategory category, String city, String search,
                                                           int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventPage = eventRepository.findPublishedEvents(
                EventStatus.PUBLISHED,
                LocalDateTime.now(),
                category,
                city,
                search,
                pageable
        );

        List<EventSummaryResponse> content = eventPage.getContent().stream()
                .map(this::toEventSummaryResponse)
                .collect(Collectors.toList());

        auditService.logEventBrowsed(
                category != null ? category.name() : null,
                city,
                search,
                page
        );

        return new PageResponse<>(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }

        auditService.logEventDetailViewed(eventId);

        return toEventDetailResponse(event);
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse getSalesSummary(UUID eventId, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        validateEventOwnership(event, organiserId);

        List<TicketTier> tiers = ticketTierRepository.findByEventId(eventId);

        List<SalesSummaryResponse.TierSalesDto> tierSales = tiers.stream()
                .map(tier -> {
                    int soldQty = tier.getTotalQty() - tier.getRemainingQty();
                    BigDecimal revenue = tier.getPrice().multiply(BigDecimal.valueOf(soldQty));
                    return new SalesSummaryResponse.TierSalesDto(
                            tier.getId(),
                            tier.getName(),
                            tier.getTotalQty(),
                            tier.getRemainingQty(),
                            soldQty,
                            revenue
                    );
                })
                .collect(Collectors.toList());

        int totalOrders = tierSales.stream().mapToInt(SalesSummaryResponse.TierSalesDto::getSoldQty).sum();
        BigDecimal totalRevenue = tierSales.stream()
                .map(SalesSummaryResponse.TierSalesDto::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        auditService.logSalesSummaryViewed(eventId, organiserId);

        return new SalesSummaryResponse(
                eventId,
                event.getTitle(),
                totalOrders,
                totalRevenue,
                tierSales
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<EventDetailResponse> getOrganizerEvents(UUID organiserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventPage = eventRepository.findByOrganiserIdOrderByCreatedAtDesc(organiserId, pageable);
        List<EventDetailResponse> content = eventPage.getContent().stream()
                .map(this::toEventDetailResponse)
                .collect(Collectors.toList());
        return new PageResponse<>(content, eventPage.getNumber(), eventPage.getSize(),
                eventPage.getTotalElements(), eventPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getAdminEventDetail(UUID eventId, UUID organiserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        validateEventOwnership(event, organiserId);
        return toEventDetailResponse(event);
    }

    private void validateEventOwnership(Event event, UUID organiserId) {
        if (!event.getOrganiserId().equals(organiserId)) {
            throw new BusinessRuleViolationException("You do not have permission to modify this event");
        }
    }

    private EventResponse toEventResponse(Event event) {
        return new EventResponse(
                event.getId(),
                event.getOrganiserId(),
                event.getVenue().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getEventDate(),
                event.getBannerImageUrl(),
                event.getStatus(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }

    private TierResponse toTierResponse(TicketTier tier) {
        return new TierResponse(
                tier.getId(),
                tier.getEvent().getId(),
                tier.getName(),
                tier.getDescription(),
                tier.getPrice(),
                tier.getTotalQty(),
                tier.getRemainingQty(),
                tier.getMaxPerOrder(),
                tier.getSaleStartsAt(),
                tier.getSaleEndsAt(),
                tier.getStatus(),
                tier.getCreatedAt()
        );
    }

    private EventSummaryResponse toEventSummaryResponse(Event event) {
        BigDecimal lowestPrice = event.getTicketTiers().stream()
                .filter(tier -> tier.getStatus() == TierStatus.ACTIVE)
                .map(TicketTier::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getEventDate(),
                event.getVenue().getCity(),
                lowestPrice,
                event.getBannerImageUrl(),
                event.getVenue().getName()
        );
    }

    private EventDetailResponse toEventDetailResponse(Event event) {
        VenueDto venueDto = new VenueDto(
                event.getVenue().getId(),
                event.getVenue().getName(),
                event.getVenue().getAddress(),
                event.getVenue().getCity(),
                event.getVenue().getCountry(),
                event.getVenue().getCapacity()
        );

        List<TierResponse> tiers = event.getTicketTiers().stream()
                .map(this::toTierResponse)
                .collect(Collectors.toList());

        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getEventDate(),
                event.getBannerImageUrl(),
                event.getStatus(),
                venueDto,
                tiers
        );
    }
}

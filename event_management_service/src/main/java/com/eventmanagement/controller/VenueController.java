package com.eventmanagement.controller;

import com.eventmanagement.dto.VenueDto;
import com.eventmanagement.entity.Venue;
import com.eventmanagement.repository.VenueRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueRepository venueRepository;

    public VenueController(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @GetMapping
    public ResponseEntity<List<VenueDto>> getAllVenues() {
        List<Venue> venues = venueRepository.findAll();
        List<VenueDto> response = venues.stream()
                .map(v -> new VenueDto(v.getId(), v.getName(), v.getAddress(), v.getCity(), v.getCountry(), v.getCapacity()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}

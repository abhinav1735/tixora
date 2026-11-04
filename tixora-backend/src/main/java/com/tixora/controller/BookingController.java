
package com.tixora.controller;

import com.tixora.dto.BookingRequest;
import com.tixora.dto.BookingResponse;
import com.tixora.dto.SeatAvailabilityResponse;
import com.tixora.service.BookingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // Get seat availability for a particular show
    @GetMapping("/shows/{showId}/seats")
    public ResponseEntity<List<SeatAvailabilityResponse>> getSeatAvailability(
            @PathVariable Long showId) {

        return ResponseEntity.ok(
                bookingService.getSeatAvailability(showId)
        );
    }

    // Create a booking
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request) {

        BookingResponse response = bookingService.createBooking(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}

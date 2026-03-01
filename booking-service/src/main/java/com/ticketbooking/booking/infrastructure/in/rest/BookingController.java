package com.ticketbooking.booking.infrastructure.in.rest;

import com.ticketbooking.booking.domain.port.in.*;
import com.ticketbooking.common.dto.BookingRequestDTO;
import com.ticketbooking.common.dto.BookingResponseDTO;
import com.ticketbooking.common.dto.PaymentRequestDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Adaptador de entrada REST.
 * Traduce HTTP → input ports. No contiene lógica de negocio.
 *
 * Inyecta las interfaces de los casos de uso (input ports), NO el ApplicationService directamente.
 * Esto permite sustituir la implementación sin tocar el controller.
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final InitiateBookingUseCase initiateBooking;
    private final ConfirmPaymentUseCase confirmPayment;
    private final CancelBookingUseCase cancelBooking;
    private final GetUserBookingsUseCase getUserBookings;

    public BookingController(InitiateBookingUseCase initiateBooking,
                             ConfirmPaymentUseCase confirmPayment,
                             CancelBookingUseCase cancelBooking,
                             GetUserBookingsUseCase getUserBookings) {
        this.initiateBooking = initiateBooking;
        this.confirmPayment = confirmPayment;
        this.cancelBooking = cancelBooking;
        this.getUserBookings = getUserBookings;
    }

    @PostMapping
    public ResponseEntity<BookingResponseDTO> initiate(@Valid @RequestBody BookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(initiateBooking.initiateBooking(request));
    }

    @PostMapping("/pay")
    public ResponseEntity<BookingResponseDTO> pay(@Valid @RequestBody PaymentRequestDTO request) {
        return ResponseEntity.ok(confirmPayment.confirmPayment(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable("id") Long id) {
        cancelBooking.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDTO>> userBookings(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(getUserBookings.getUserBookings(userId));
    }
}

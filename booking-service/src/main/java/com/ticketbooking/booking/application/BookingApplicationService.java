package com.ticketbooking.booking.application;

import com.ticketbooking.booking.domain.model.Booking;
import com.ticketbooking.booking.domain.model.Money;
import com.ticketbooking.booking.domain.model.enums.BookingStatus;
import com.ticketbooking.booking.domain.port.in.*;
import com.ticketbooking.booking.domain.port.out.*;
import com.ticketbooking.common.dto.BookingRequestDTO;
import com.ticketbooking.common.dto.BookingResponseDTO;
import com.ticketbooking.common.dto.PaymentRequestDTO;
import com.ticketbooking.common.exception.BookingNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Application Service: implementa todos los input ports del bounded context de Reservas.
 *
 * Responsabilidad: orquestar el flujo usando los output ports.
 * NO contiene lógica de negocio — esa vive en Booking.confirm(), cancel(), etc.
 *
 * La lógica de negocio (invariantes) está en el agregado Booking.
 * Este servicio se encarga de la coordinación entre puertos.
 */
@Service
public class BookingApplicationService
        implements InitiateBookingUseCase,
                   ConfirmPaymentUseCase,
                   CancelBookingUseCase,
                   ReleaseExpiredBookingsUseCase,
                   GetUserBookingsUseCase {

    private static final int SEAT_LOCK_TTL_MINUTES = 5;

    private final BookingRepositoryPort bookingRepository;
    private final SeatLockingPort seatLocking;
    private final PaymentPort payment;
    private final NotificationPort notification;

    public BookingApplicationService(BookingRepositoryPort bookingRepository,
                                     SeatLockingPort seatLocking,
                                     PaymentPort payment,
                                     NotificationPort notification) {
        this.bookingRepository = bookingRepository;
        this.seatLocking = seatLocking;
        this.payment = payment;
        this.notification = notification;
    }

    @Override
    public BookingResponseDTO initiateBooking(BookingRequestDTO request) {
        // 1. Lockear el asiento (puede lanzar SeatNotAvailableException)
        seatLocking.lock(request.getSeatId());

        // 2. Crear el agregado Booking (lógica de creación en el constructor del dominio)
        Booking booking = new Booking(
                request.getUserId(),
                request.getEventId(),
                request.getSeatId(),
                Money.of(BigDecimal.TEN), // TODO: obtener precio real del event-management-service
                LocalDateTime.now().plusMinutes(SEAT_LOCK_TTL_MINUTES)
        );

        // 3. Persistir
        Booking saved = bookingRepository.save(booking);

        return toResponse(saved);
    }

    @Override
    public BookingResponseDTO confirmPayment(PaymentRequestDTO request) {
        // 1. Cargar el agregado
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new BookingNotFoundException(request.getBookingId()));

        // 2. Procesar pago (output port — puede lanzar excepción si falla)
        String transactionId = payment.processPayment(booking.getId(), request.getPaymentToken());

        // 3. Confirmar el agregado (la invariante vive en el dominio)
        booking.confirm(transactionId);

        // 4. Confirmar el asiento en seat-inventory-service
        seatLocking.confirm(booking.getSeatId());

        // 5. Persistir estado actualizado
        Booking saved = bookingRepository.save(booking);

        // 6. Notificar de forma async (no bloquea el flujo crítico)
        notification.notifyConfirmation(saved.getId());

        return toResponse(saved);
    }

    @Override
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));

        // Lógica de negocio en el agregado
        booking.cancel();

        seatLocking.release(booking.getSeatId());
        bookingRepository.save(booking);
        notification.notifyCancellation(booking.getId());
    }

    @Override
    public void releaseExpiredBookings() {
        bookingRepository.findExpiredPendingBookings(LocalDateTime.now())
                .forEach(booking -> {
                    booking.expire(); // lógica en el agregado
                    seatLocking.release(booking.getSeatId());
                    bookingRepository.save(booking);
                });
    }

    @Override
    public List<BookingResponseDTO> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─── Mapper dominio → DTO (vive en application, no en domain) ────────────

    private BookingResponseDTO toResponse(Booking booking) {
        return BookingResponseDTO.builder()
                .bookingId(booking.getId())
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice().getAmount())
                .expiresAt(booking.getExpiresAt())
                .bookingTime(booking.getBookingTime())
                .build();
    }
}

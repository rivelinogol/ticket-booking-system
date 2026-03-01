package com.ticketbooking.booking.infrastructure.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA. @Entity vive AQUÍ, no en el dominio.
 * Es un detalle de infraestructura — si cambiamos de JPA a MongoDB,
 * solo cambia este archivo y el adaptador. El dominio no se toca.
 */
@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private Long seatId;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private String status;

    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookingTime;

    private LocalDateTime updatedAt;

    private String paymentTransactionId;
}

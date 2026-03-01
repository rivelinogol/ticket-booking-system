package com.ticketbooking.event.infrastructure.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String description;
    @Column(nullable = false) private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal basePrice;
    @Column(nullable = false) private String status;
    @Column(name = "venue_id") private Long venueId;
}

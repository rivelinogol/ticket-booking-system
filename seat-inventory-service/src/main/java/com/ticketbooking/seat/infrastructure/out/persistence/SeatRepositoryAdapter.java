package com.ticketbooking.seat.infrastructure.out.persistence;

import com.ticketbooking.seat.domain.model.Seat;
import com.ticketbooking.seat.domain.model.SeatPosition;
import com.ticketbooking.seat.domain.model.enums.SeatStatus;
import com.ticketbooking.seat.domain.port.out.SeatRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SeatRepositoryAdapter implements SeatRepositoryPort {

    private final SeatJpaRepository jpaRepository;

    public SeatRepositoryAdapter(SeatJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Seat save(Seat seat) {
        return toDomain(jpaRepository.save(toJpa(seat)));
    }

    @Override
    public Optional<Seat> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Seat> findByIdWithLock(Long id) {
        return jpaRepository.findByIdWithLock(id).map(this::toDomain);
    }

    @Override
    public List<Seat> findByVenueIdAndStatus(Long venueId, SeatStatus status) {
        return jpaRepository.findByVenueIdAndStatus(venueId, status.name())
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    private SeatJpaEntity toJpa(Seat s) {
        return SeatJpaEntity.builder()
                .id(s.getId())
                .venueId(s.getVenueId())
                .rowNumber(s.getPosition().getRowNumber())
                .seatNumber(s.getPosition().getSeatNumber())
                .section(s.getSection())
                .price(s.getPrice())
                .status(s.getStatus().name())
                .version(s.getVersion())
                .build();
    }

    private Seat toDomain(SeatJpaEntity e) {
        return new Seat(
                e.getId(),
                e.getVenueId(),
                new SeatPosition(e.getRowNumber(), e.getSeatNumber()),
                e.getSection(),
                e.getPrice(),
                SeatStatus.valueOf(e.getStatus()),
                e.getVersion()
        );
    }
}

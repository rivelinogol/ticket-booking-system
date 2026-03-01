package com.ticketbooking.seat.application;

import com.ticketbooking.common.dto.SeatAvailabilityDTO;
import com.ticketbooking.common.exception.SeatNotAvailableException;
import com.ticketbooking.seat.domain.model.Seat;
import com.ticketbooking.seat.domain.model.enums.SeatStatus;
import com.ticketbooking.seat.domain.port.in.*;
import com.ticketbooking.seat.domain.port.out.SeatLockPort;
import com.ticketbooking.seat.domain.port.out.SeatRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatInventoryApplicationService
        implements LockSeatUseCase,
                   ConfirmSeatUseCase,
                   ReleaseSeatUseCase,
                   GetAvailableSeatsUseCase {

    private static final int LOCK_TTL_MINUTES = 5;

    private final SeatRepositoryPort seatRepository;
    private final SeatLockPort seatLock;

    public SeatInventoryApplicationService(SeatRepositoryPort seatRepository,
                                           SeatLockPort seatLock) {
        this.seatRepository = seatRepository;
        this.seatLock = seatLock;
    }

    @Override
    public void lockSeat(Long seatId) {
        // 1. Intentar adquirir el lock distribuido (Redis en prod, ConcurrentHashMap en dev)
        boolean locked = seatLock.tryLock(seatId, LOCK_TTL_MINUTES);
        if (!locked) {
            throw new SeatNotAvailableException(seatId);
        }

        // 2. Cargar y mutar el agregado (lógica de negocio en Seat.lock())
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

        seat.lock(); // lanza IllegalStateException si no está AVAILABLE
        seatRepository.save(seat);
    }

    @Override
    public void confirmSeat(Long seatId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

        seat.confirm(); // lanza IllegalStateException si no está LOCKED
        seatLock.unlock(seatId);
        seatRepository.save(seat);
    }

    @Override
    public void releaseSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

        seat.release();
        seatLock.unlock(seatId);
        seatRepository.save(seat);
    }

    @Override
    public List<SeatAvailabilityDTO> getAvailableSeats(Long venueId) {
        return seatRepository.findByVenueIdAndStatus(venueId, SeatStatus.AVAILABLE)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private SeatAvailabilityDTO toDTO(Seat seat) {
        return SeatAvailabilityDTO.builder()
                .seatId(seat.getId())
                .rowNumber(seat.getPosition().getRowNumber())
                .seatNumber(seat.getPosition().getSeatNumber())
                .section(seat.getSection())
                .price(seat.getPrice())
                .status(seat.getStatus().name())
                .build();
    }
}

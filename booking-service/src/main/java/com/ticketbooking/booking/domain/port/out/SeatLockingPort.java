package com.ticketbooking.booking.domain.port.out;

/**
 * Output port: contrato para lockear/liberar asientos.
 * El dominio no sabe si la implementación usa Redis, HTTP o DB locks.
 * Lo implementa SeatInventoryClientAdapter en infrastructure/out/client/.
 */
public interface SeatLockingPort {
    void lock(Long seatId);
    void confirm(Long seatId);
    void release(Long seatId);
}

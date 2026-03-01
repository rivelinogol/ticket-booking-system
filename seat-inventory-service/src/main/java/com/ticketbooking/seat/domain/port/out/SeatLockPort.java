package com.ticketbooking.seat.domain.port.out;

/**
 * Output port: contrato para el lock distribuido de asientos.
 * El dominio no sabe si la implementación usa Redis, Hazelcast o in-memory.
 * En producción: Redis SET NX con TTL de 5 minutos.
 * En dev: ConcurrentHashMap (InMemorySeatLockAdapter).
 */
public interface SeatLockPort {
    boolean tryLock(Long seatId, int ttlMinutes);
    void unlock(Long seatId);
    boolean isLocked(Long seatId);
}

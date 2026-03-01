package com.ticketbooking.seat.infrastructure.out.cache;

import com.ticketbooking.seat.domain.port.out.SeatLockPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador de salida: implementa SeatLockPort con ConcurrentHashMap para desarrollo.
 *
 * En producción se reemplazaría con RedisSeatLockAdapter:
 *   redisTemplate.opsForValue().setIfAbsent("seat:lock:" + seatId, "locked",
 *       Duration.ofMinutes(ttlMinutes))
 *
 * La ventaja de tener SeatLockPort como interfaz: podemos cambiar la implementación
 * de in-memory a Redis sin tocar el dominio ni el application service.
 */
@Component
@ConditionalOnProperty(
        name = "app.seat-lock.provider",
        havingValue = "in-memory",
        matchIfMissing = true
)
public class InMemorySeatLockAdapter implements SeatLockPort {

    // seatId → expiration time
    private final ConcurrentHashMap<Long, LocalDateTime> locks = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(Long seatId, int ttlMinutes) {
        // Limpiar lock expirado si existe
        LocalDateTime existing = locks.get(seatId);
        if (existing != null && LocalDateTime.now().isBefore(existing)) {
            return false; // ya está lockeado y no expiró
        }
        locks.put(seatId, LocalDateTime.now().plusMinutes(ttlMinutes));
        return true;
    }

    @Override
    public void unlock(Long seatId) {
        locks.remove(seatId);
    }

    @Override
    public boolean isLocked(Long seatId) {
        LocalDateTime expiry = locks.get(seatId);
        return expiry != null && LocalDateTime.now().isBefore(expiry);
    }
}

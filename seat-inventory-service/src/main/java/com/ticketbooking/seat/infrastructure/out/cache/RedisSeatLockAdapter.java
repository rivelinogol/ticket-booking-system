package com.ticketbooking.seat.infrastructure.out.cache;

import com.ticketbooking.seat.domain.port.out.SeatLockPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.seat-lock.provider", havingValue = "redis")
public class RedisSeatLockAdapter implements SeatLockPort {

    private static final String LOCK_KEY_PREFIX = "seat:lock:";

    private final StringRedisTemplate redisTemplate;

    public RedisSeatLockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(Long seatId, int ttlMinutes) {
        String key = LOCK_KEY_PREFIX + seatId;
        Duration ttl = Duration.ofMinutes(ttlMinutes);
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "locked", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(Long seatId) {
        redisTemplate.delete(LOCK_KEY_PREFIX + seatId);
    }

    @Override
    public boolean isLocked(Long seatId) {
        Boolean exists = redisTemplate.hasKey(LOCK_KEY_PREFIX + seatId);
        return Boolean.TRUE.equals(exists);
    }
}

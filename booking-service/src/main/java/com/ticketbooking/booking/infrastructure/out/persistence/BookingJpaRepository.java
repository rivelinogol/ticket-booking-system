package com.ticketbooking.booking.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, Long> {

    List<BookingJpaEntity> findByUserId(Long userId);

    @Query("SELECT b FROM BookingJpaEntity b WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    List<BookingJpaEntity> findExpiredPending(@Param("now") LocalDateTime now);
}

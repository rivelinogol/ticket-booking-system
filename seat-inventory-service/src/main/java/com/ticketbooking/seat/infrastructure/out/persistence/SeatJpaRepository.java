package com.ticketbooking.seat.infrastructure.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatJpaRepository extends JpaRepository<SeatJpaEntity, Long> {

    List<SeatJpaEntity> findByVenueIdAndStatus(Long venueId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatJpaEntity s WHERE s.id = :id")
    Optional<SeatJpaEntity> findByIdWithLock(@Param("id") Long id);
}

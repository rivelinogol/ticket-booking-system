package com.ticketbooking.event.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EventJpaRepository extends JpaRepository<EventJpaEntity, Long> {
    List<EventJpaEntity> findByStatus(String status);

    @Query("SELECT e FROM EventJpaEntity e JOIN VenueJpaEntity v ON e.venueId = v.id WHERE v.city = :city AND e.status = :status")
    List<EventJpaEntity> findByVenueCityAndStatus(@Param("city") String city, @Param("status") String status);
}

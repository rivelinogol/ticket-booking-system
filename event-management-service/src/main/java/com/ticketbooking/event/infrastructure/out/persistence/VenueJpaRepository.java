package com.ticketbooking.event.infrastructure.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueJpaRepository extends JpaRepository<VenueJpaEntity, Long> {}

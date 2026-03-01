package com.ticketbooking.auth.domain.port.out;

import com.ticketbooking.auth.domain.model.User;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

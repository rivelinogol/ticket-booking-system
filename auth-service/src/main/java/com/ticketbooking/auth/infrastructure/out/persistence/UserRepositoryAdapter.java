package com.ticketbooking.auth.infrastructure.out.persistence;

import com.ticketbooking.auth.domain.model.Email;
import com.ticketbooking.auth.domain.model.User;
import com.ticketbooking.auth.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) { this.jpa = jpa; }

    @Override
    public User save(User u) { return toDomain(jpa.save(toJpa(u))); }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) { return jpa.existsByEmail(email); }

    private UserJpaEntity toJpa(User u) {
        return UserJpaEntity.builder()
                .id(u.getId()).email(u.getEmail().getValue())
                .name(u.getName()).phone(u.getPhone())
                .passwordHash(u.getPasswordHash()).build();
    }

    private User toDomain(UserJpaEntity e) {
        return new User(e.getId(), new Email(e.getEmail()), e.getName(), e.getPhone(), e.getPasswordHash());
    }
}

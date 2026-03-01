package com.ticketbooking.auth.application;

import com.ticketbooking.auth.domain.model.Email;
import com.ticketbooking.auth.domain.model.User;
import com.ticketbooking.auth.domain.port.in.LoginUseCase;
import com.ticketbooking.auth.domain.port.in.RegisterUseCase;
import com.ticketbooking.auth.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements RegisterUseCase, LoginUseCase {

    private final UserRepositoryPort userRepository;

    public AuthApplicationService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Long register(String emailStr, String password, String name) {
        Email email = new Email(emailStr); // Value Object valida formato aquí
        if (userRepository.existsByEmail(email.getValue())) {
            throw new IllegalArgumentException("Email already registered: " + emailStr);
        }
        // TODO: hashear password con BCrypt
        User user = new User(email, name, "hashed_" + password);
        return userRepository.save(user).getId();
    }

    @Override
    public String login(String emailStr, String password) {
        User user = userRepository.findByEmail(emailStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        // TODO: verificar BCrypt + generar JWT
        return "jwt_stub_token_for_" + user.getId();
    }
}

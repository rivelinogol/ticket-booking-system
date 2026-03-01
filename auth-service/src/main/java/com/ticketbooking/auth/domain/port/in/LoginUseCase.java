package com.ticketbooking.auth.domain.port.in;

public interface LoginUseCase {
    /** Retorna el JWT token si las credenciales son válidas. */
    String login(String email, String password);
}

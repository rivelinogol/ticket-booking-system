package com.ticketbooking.auth.domain.port.in;

public interface RegisterUseCase {
    Long register(String email, String password, String name);
}

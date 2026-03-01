package com.ticketbooking.auth.infrastructure.in.rest;

import com.ticketbooking.auth.domain.port.in.LoginUseCase;
import com.ticketbooking.auth.domain.port.in.RegisterUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegisterUseCase register;
    private final LoginUseCase login;

    public AuthController(RegisterUseCase register, LoginUseCase login) {
        this.register = register;
        this.login = login;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Long>> register(@RequestBody Map<String, String> body) {
        Long userId = register.register(body.get("email"), body.get("password"), body.get("name"));
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String token = login.login(body.get("email"), body.get("password"));
        return ResponseEntity.ok(Map.of("token", token));
    }
}

package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.dto.AuthRequest;
import com.rachel.ledgercore.dto.AuthResponse;
import com.rachel.ledgercore.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest authRequest){
        AuthResponse response = authService.register(authRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest){
        AuthResponse response = authService.login(authRequest);
        return ResponseEntity.ok(response);
    }

}

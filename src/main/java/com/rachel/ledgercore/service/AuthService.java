package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.AuthRequest;
import com.rachel.ledgercore.dto.AuthResponse;
import com.rachel.ledgercore.exception.UserAlreadyExistingException;
import com.rachel.ledgercore.exception.UserNotFoundException;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse register(AuthRequest authRequest){
        if(userRepository.findByUsername(authRequest.getUserName()).isPresent()){
            throw new UserAlreadyExistingException("User already exists");
        }
        User user = new User();
        user.setUsername(authRequest.getUserName());
        user.setPassword(passwordEncoder.encode(authRequest.getPassword()));
        userRepository.save(user);
        return AuthResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .message("User Registered successfully!")
                .build();
    }

    public AuthResponse login(AuthRequest authRequest){
        User user = userRepository.findByUsername(authRequest.getUserName())
                .orElseThrow(() -> new UserNotFoundException("User not existing!"));

        if (!passwordEncoder.matches(
                authRequest.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid username or password");
        }

        return AuthResponse.builder()
                .id(user.getId())
                .userName(user.getUsername())
                .message("Logged in successfully!")
                .build();
    }

}

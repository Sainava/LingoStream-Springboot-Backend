package com.lingostream.core.controller;

import com.lingostream.core.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // In a full production system, this checks PostgreSQL. For now, we mock the auth.
        if ("admin".equals(username) && "password".equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok(token);
        }

        return ResponseEntity.status(401).body("Invalid credentials");
    }
}
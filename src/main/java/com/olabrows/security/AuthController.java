package com.olabrows.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${admin.username}")
    private String adminUsername;
    @Value("${admin.password.hash}")
    private String adminPasswordHash;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (adminUsername.equals(username) && password != null && !password.isBlank() && encoder.matches(password, adminPasswordHash)) {
            String token = jwtUtil.generateToken(username);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", username);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
}

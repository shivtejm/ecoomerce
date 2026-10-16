package com.example.RegisterPage.controller;

import com.example.RegisterPage.dto.ForgotPasswordRequest;
import com.example.RegisterPage.dto.LoginRequest;
import com.example.RegisterPage.dto.ProfileResponse;
import com.example.RegisterPage.dto.RegisterRequest;
import com.example.RegisterPage.dto.ResetPasswordRequest;
import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.service.AuthService;
import com.example.RegisterPage.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String result = authService.register(request);

        if ("Email already registered".equals(result)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Email is already registered"));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {

        String result = authService.login(request);

        if (!"Login successful".equals(result)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        String token = jwtService.generateToken(request.getEmail());

        ResponseCookie cookie = ResponseCookie.from("JWT", token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(3600)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        User user = authService.getUserByEmail(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole() != null ? user.getRole() : "ROLE_USER",
                "token", token
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("JWT", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Not authenticated"));
        }

        String email = authentication.getName();
        User user = authService.getUserByEmail(email);

        return ResponseEntity.ok(new ProfileResponse(
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole() : "ROLE_USER"
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                           @RequestHeader(value = "Origin", required = false) String origin) {
        String frontendUrl = (origin != null && !origin.isBlank()) ? origin : "http://localhost:4200";
        authService.processForgotPassword(request.getEmail(), frontendUrl);
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists with that email, a password reset link has been sent."
        ));
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestParam("token") String token) {
        boolean valid = authService.validateResetToken(token);
        if (valid) {
            return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("valid", false, "message", "Reset link is invalid or has expired"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String result = authService.resetPassword(request.getToken(), request.getNewPassword());
        if ("Password successfully reset".equals(result)) {
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully. You can now log in."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", result));
        }
    }
}
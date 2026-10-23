package com.example.RegisterPage.service;

import com.example.RegisterPage.dto.LoginRequest;
import com.example.RegisterPage.dto.RegisterRequest;
import com.example.RegisterPage.entity.PasswordResetToken;
import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.repository.PasswordResetTokenRepository;
import com.example.RegisterPage.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    public String register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email already registered";
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        if ("ADMIN2026".equals(request.getAdminCode())) {
            user.setRole("ROLE_ADMIN");
        } else {
            user.setRole("ROLE_USER");
        }

        userRepository.save(user);

        return "Registration successful";
    }

    public String login(LoginRequest request) {

        User existingUser =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElse(null);

        if (existingUser == null) {
            return "User not found";
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                existingUser.getPassword())) {

            return "Invalid password";
        }

        return "Login successful";
    }

    public User getUserByEmail(String email) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(
                        () -> new RuntimeException(
                                "User not found"
                        )
                );
    }

    @Transactional
    public void processForgotPassword(String email, String frontendBaseUrl) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Return silently to prevent email enumeration attacks
            return;
        }

        User user = userOpt.get();

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUser(user);

        // Generate a new secure token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);

        String resetUrl = (frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:4200") + "/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
    }

    public boolean validateResetToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        return !resetToken.isUsed() && !resetToken.isExpired();
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            return "Token is required";
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return "Invalid reset token";
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed()) {
            return "Token has already been used";
        }

        if (resetToken.isExpired()) {
            return "Reset token has expired";
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the tok   en so it cannot be reused
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return "Password successfully reset";
    }
}
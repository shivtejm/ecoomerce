package com.example.RegisterPage.repository;

import com.example.RegisterPage.entity.PasswordResetToken;
import com.example.RegisterPage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}

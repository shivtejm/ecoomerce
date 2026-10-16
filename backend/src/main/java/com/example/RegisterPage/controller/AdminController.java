package com.example.RegisterPage.controller;

import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private User getAuthenticatedAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        User user = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return null;
        }
        return user;
    }

    /**
     * GET /api/admin/users
     * Lists all users and their roles (Admin only).
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Administrator privileges required"));
        }

        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "role", u.getRole() != null ? u.getRole() : "ROLE_USER"
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    /**
     * PUT /api/admin/users/{id}/role
     * Allows an admin to promote or demote any user (Admin only).
     */
    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id,
                                            @RequestParam String newRole,
                                            Authentication authentication) {
        User admin = getAuthenticatedAdmin(authentication);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Access denied: Administrator privileges required"));
        }

        User targetUser = userRepository.findById(id).orElse(null);
        if (targetUser == null) {
            return ResponseEntity.notFound().build();
        }

        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        return ResponseEntity.ok(Map.of(
                "message", "User " + targetUser.getEmail() + " role updated to " + newRole,
                "role", newRole
        ));
    }
}

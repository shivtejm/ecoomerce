package com.example.RegisterPage.controller;

import com.example.RegisterPage.dto.CreateOrderRequest;
import com.example.RegisterPage.dto.OrderRecordDTO;
import com.example.RegisterPage.dto.OrderResponse;
import com.example.RegisterPage.dto.PaymentVerificationRequest;
import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.service.AuthService;
import com.example.RegisterPage.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final AuthService authService;

    public PaymentController(PaymentService paymentService, AuthService authService) {
        this.paymentService = paymentService;
        this.authService = authService;
    }

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                         Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "You must be logged in to make a purchase"));
        }

        try {
            User user = authService.getUserByEmail(authentication.getName());
            OrderResponse response = paymentService.createRazorpayOrder(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to create order"));
        }
    }

    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request,
                                           Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "You must be logged in to verify payment"));
        }

        try {
            User user = authService.getUserByEmail(authentication.getName());
            OrderRecordDTO completedOrder = paymentService.verifyAndCompletePayment(request, user);
            return ResponseEntity.ok(Map.of(
                    "message", "Payment verified and order placed successfully",
                    "order", completedOrder
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Payment verification failed"));
        }
    }

    @PostMapping("/create-cart-order")
    public ResponseEntity<?> createCartOrder(@Valid @RequestBody com.example.RegisterPage.dto.CreateCartOrderRequest request,
                                            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "You must be logged in to make a purchase"));
        }

        try {
            User user = authService.getUserByEmail(authentication.getName());
            OrderResponse response = paymentService.createCartRazorpayOrder(request, user);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Failed to create cart order"));
        }
    }

    @PostMapping("/verify-cart-payment")
    public ResponseEntity<?> verifyCartPayment(@Valid @RequestBody com.example.RegisterPage.dto.VerifyCartPaymentRequest request,
                                               Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "You must be logged in to verify payment"));
        }

        try {
            User user = authService.getUserByEmail(authentication.getName());
            List<OrderRecordDTO> completedOrders = paymentService.verifyAndCompleteCartPayment(request, user);
            return ResponseEntity.ok(Map.of(
                    "message", "Cart payment verified and orders placed successfully",
                    "orders", completedOrders
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Cart payment verification failed"));
        }
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "You must be logged in to view your orders"));
        }

        User user = authService.getUserByEmail(authentication.getName());
        List<OrderRecordDTO> orders = paymentService.getUserOrders(user);
        return ResponseEntity.ok(orders);
    }
}

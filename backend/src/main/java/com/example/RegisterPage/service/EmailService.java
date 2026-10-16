package com.example.RegisterPage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        String border = "================================================================================";
        System.out.println("\n" + border);
        System.out.println("PASSWORD RESET LINK GENERATED");
        System.out.println("Recipient Email : " + toEmail);
        System.out.println("Reset Link      : " + resetUrl);
        System.out.println("Validity        : 15 minutes");
        System.out.println(border + "\n");

        log.info("Password reset link generated for {}: {}", toEmail, resetUrl);
    }
}

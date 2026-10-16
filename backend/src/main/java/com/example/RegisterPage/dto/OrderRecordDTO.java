package com.example.RegisterPage.dto;

import com.example.RegisterPage.entity.OrderRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderRecordDTO {

    private Long id;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;
    private LocalDateTime createdAt;

    public OrderRecordDTO() {
    }

    public OrderRecordDTO(OrderRecord order) {
        this.id = order.getId();
        this.razorpayOrderId = order.getRazorpayOrderId();
        this.razorpayPaymentId = order.getRazorpayPaymentId();
        this.amount = order.getAmount();
        this.currency = order.getCurrency();
        this.status = order.getStatus();
        if (order.getProduct() != null) {
            this.productId = order.getProduct().getId();
            this.productName = order.getProduct().getName();
            this.productImageUrl = order.getProduct().getImageUrl();
        }
        this.quantity = order.getQuantity();
        this.createdAt = order.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductImageUrl() {
        return productImageUrl;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

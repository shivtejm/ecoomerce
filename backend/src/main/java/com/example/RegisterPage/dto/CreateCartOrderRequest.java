package com.example.RegisterPage.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class CreateCartOrderRequest {

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<CartItemDTO> items;

    public CreateCartOrderRequest() {
    }

    public CreateCartOrderRequest(List<CartItemDTO> items) {
        this.items = items;
    }

    public List<CartItemDTO> getItems() {
        return items;
    }

    public void setItems(List<CartItemDTO> items) {
        this.items = items;
    }
}

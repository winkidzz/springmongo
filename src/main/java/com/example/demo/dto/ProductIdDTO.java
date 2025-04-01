package com.example.demo.dto;

import org.springframework.data.annotation.Id;

public class ProductIdDTO {
    @Id
    private String productId;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
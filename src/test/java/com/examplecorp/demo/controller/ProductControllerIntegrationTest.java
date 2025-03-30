package com.examplecorp.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCreateAndGetProduct() throws Exception {
        String productJson = "{\"id\":\"1\",\"name\":\"Test Product\",\"price\":99.99,\"description\":\"Test Description\"}";

        // Create product
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Test Product")))
                .andExpect(jsonPath("$.price", is(99.99)))
                .andExpect(jsonPath("$.description", is("Test Description")));

        // Get product
        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.name", is("Test Product")))
                .andExpect(jsonPath("$.price", is(99.99)))
                .andExpect(jsonPath("$.description", is("Test Description")));
    }

    @Test
    public void testGetNonExistentProduct() throws Exception {
        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateProduct() throws Exception {
        // First create a product
        String createJson = "{\"id\":\"2\",\"name\":\"Original Product\",\"price\":50.00,\"description\":\"Original Description\"}";
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk());

        // Then update it
        String updateJson = "{\"id\":\"2\",\"name\":\"Updated Product\",\"price\":75.00,\"description\":\"Updated Description\"}";
        mockMvc.perform(put("/api/products/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Product")))
                .andExpect(jsonPath("$.price", is(75.00)))
                .andExpect(jsonPath("$.description", is("Updated Description")));
    }

    @Test
    public void testDeleteProduct() throws Exception {
        // First create a product
        String createJson = "{\"id\":\"3\",\"name\":\"Product to Delete\",\"price\":100.00,\"description\":\"Delete me\"}";
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk());

        // Then delete it
        mockMvc.perform(delete("/api/products/3"))
                .andExpect(status().isOk());

        // Verify it's deleted
        mockMvc.perform(get("/api/products/3"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetAllProducts() throws Exception {
        // Create multiple products
        String product1Json = "{\"id\":\"4\",\"name\":\"Product 1\",\"price\":10.00,\"description\":\"First product\"}";
        String product2Json = "{\"id\":\"5\",\"name\":\"Product 2\",\"price\":20.00,\"description\":\"Second product\"}";

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(product1Json))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(product2Json))
                .andExpect(status().isOk());

        // Get all products
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("4")))
                .andExpect(jsonPath("$", hasKey("5")))
                .andExpect(jsonPath("$.4.name", is("Product 1")))
                .andExpect(jsonPath("$.5.name", is("Product 2")));
    }
} 
package com.example.demo.controller;

import com.example.demo.model.ProductConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
public class ProductConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        // Create a product first
        String productJson = "{\"id\":\"1\",\"name\":\"Test Product\",\"price\":99.99,\"description\":\"Test Description\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetAllConfigs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/configs")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testGetActiveConfigs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/configs/active")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testCreateAndGetConfig() throws Exception {
        String configJson = "{\"id\":\"1\",\"productId\":\"1\",\"releaseStartDate\":\"2025-01-01T00:00:00\",\"releaseEndDate\":\"2025-12-31T23:59:59\",\"enabled\":true,\"createdStartDate\":\"2025-01-01T00:00:00\",\"createdEndDate\":\"2025-12-31T23:59:59\"}";

        // Create config
        mockMvc.perform(MockMvcRequestBuilders.post("/api/product-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(configJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.productId", is("1")))
                .andExpect(jsonPath("$.enabled", is(true)));

        // Get config
        mockMvc.perform(MockMvcRequestBuilders.get("/api/product-configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.productId", is("1")))
                .andExpect(jsonPath("$.enabled", is(true)));
    }

    @Test
    public void testGetNonExistentConfig() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/product-configs/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateConfig() throws Exception {
        // First create a config
        String createJson = "{\"id\":\"2\",\"productId\":\"1\",\"releaseStartDate\":\"2025-01-01T00:00:00\",\"releaseEndDate\":\"2025-12-31T23:59:59\",\"enabled\":true,\"createdStartDate\":\"2025-01-01T00:00:00\",\"createdEndDate\":\"2025-12-31T23:59:59\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/product-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk());

        // Then update it
        String updateJson = "{\"id\":\"2\",\"productId\":\"1\",\"releaseStartDate\":\"2025-01-01T00:00:00\",\"releaseEndDate\":\"2025-12-31T23:59:59\",\"enabled\":false,\"createdStartDate\":\"2025-01-01T00:00:00\",\"createdEndDate\":\"2025-12-31T23:59:59\"}";
        mockMvc.perform(MockMvcRequestBuilders.put("/api/product-configs/2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled", is(false)));
    }

    @Test
    public void testDeleteConfig() throws Exception {
        // First create a config
        String createJson = "{\"id\":\"3\",\"productId\":\"1\",\"releaseStartDate\":\"2025-01-01T00:00:00\",\"releaseEndDate\":\"2025-12-31T23:59:59\",\"enabled\":true,\"createdStartDate\":\"2025-01-01T00:00:00\",\"createdEndDate\":\"2025-12-31T23:59:59\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/api/product-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createJson))
                .andExpect(status().isOk());

        // Then delete it
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/product-configs/3"))
                .andExpect(status().isOk());

        // Verify it's deleted
        mockMvc.perform(MockMvcRequestBuilders.get("/api/product-configs/3"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetConfigsByProduct() throws Exception {
        // Create multiple configs for the same product
        String config1Json = "{\"id\":\"4\",\"productId\":\"1\",\"releaseStartDate\":\"2025-01-01T00:00:00\",\"releaseEndDate\":\"2025-12-31T23:59:59\",\"enabled\":true,\"createdStartDate\":\"2025-01-01T00:00:00\",\"createdEndDate\":\"2025-12-31T23:59:59\"}";
        String config2Json = "{\"id\":\"5\",\"productId\":\"1\",\"releaseStartDate\":\"2025-01-01T00:00:00\",\"releaseEndDate\":\"2025-12-31T23:59:59\",\"enabled\":true,\"createdStartDate\":\"2025-01-01T00:00:00\",\"createdEndDate\":\"2025-12-31T23:59:59\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/product-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(config1Json))
                .andExpect(status().isOk());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/product-configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(config2Json))
                .andExpect(status().isOk());

        // Get configs by product
        mockMvc.perform(MockMvcRequestBuilders.get("/api/product-configs/product/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasKey("4")))
                .andExpect(jsonPath("$", hasKey("5")))
                .andExpect(jsonPath("$.4.productId", is("1")))
                .andExpect(jsonPath("$.5.productId", is("1")));
    }
} 
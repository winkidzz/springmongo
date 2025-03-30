package com.example.demo.service;

import com.example.demo.controller.ProductController.Product;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductService {
    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public Product createProduct(Product product) {
        if (product != null && product.getId() != null) {
            products.put(product.getId(), product);
            return product;
        }
        return null;
    }

    public Product getProduct(String id) {
        return products.get(id);
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public Product updateProduct(String id, Product product) {
        if (id != null && products.containsKey(id)) {
            products.put(id, product);
            return product;
        }
        return null;
    }

    public void deleteProduct(String id) {
        if (id != null) {
            products.remove(id);
        }
    }

    public void clearProducts() {
        products.clear();
    }

    public void addProduct(String id, Product product) {
        if (id != null && product != null) {
            products.put(id, product);
        }
    }

    public int getProductCount() {
        return products.size();
    }
}
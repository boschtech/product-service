package com.boschtech.productservice.service;

import com.boschtech.productservice.model.Product;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductService {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Seed some sample data
        createProduct(new Product("Wireless Keyboard", "Bluetooth mechanical keyboard", new BigDecimal("79.99"), "Electronics"));
        createProduct(new Product("Running Shoes", "Lightweight trail running shoes", new BigDecimal("129.99"), "Footwear"));
        createProduct(new Product("Coffee Maker", "12-cup programmable coffee maker", new BigDecimal("49.99"), "Kitchen"));
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public Optional<Product> getProductById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    public Product createProduct(Product product) {
        products.put(product.getId(), product);
        return product;
    }

    public Optional<Product> updateProduct(String id, Product updated) {
        if (!products.containsKey(id)) {
            return Optional.empty();
        }
        updated.setId(id);
        products.put(id, updated);
        return Optional.of(updated);
    }

    public boolean deleteProduct(String id) {
        return products.remove(id) != null;
    }
}

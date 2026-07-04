package com.boschtech.productservice.service;

import com.boschtech.productservice.model.Product;
import com.boschtech.productservice.repository.ProductRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostConstruct
    public void init() {
        if (productRepository.count() == 0) {
            productRepository.save(new Product("Wireless Keyboard", "Bluetooth mechanical keyboard", new BigDecimal("79.99"), "Electronics"));
            productRepository.save(new Product("Running Shoes", "Lightweight trail running shoes", new BigDecimal("129.99"), "Footwear"));
            productRepository.save(new Product("Coffee Maker", "12-cup programmable coffee maker", new BigDecimal("49.99"), "Kitchen"));
        }
    }

    public List<Product> getAllProducts(String search) {
        if (search == null || search.isBlank()) {
            return productRepository.findAll();
        }
        return productRepository.search(search.trim());
    }

    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        if (product.getId() != null && product.getId().isBlank()) {
            product.setId(null);
        }
        return productRepository.save(product);
    }

    public Optional<Product> updateProduct(String id, Product updated) {
        if (!productRepository.existsById(id)) {
            return Optional.empty();
        }
        updated.setId(id);
        return Optional.of(productRepository.save(updated));
    }

    public boolean deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            return false;
        }
        productRepository.deleteById(id);
        return true;
    }
}

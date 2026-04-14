package com.boschtech.productservice.service;

import com.boschtech.productservice.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ProductServiceTest {

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService();
    }

    @Test
    void init_shouldSeedThreeProducts() {
        productService.init();
        List<Product> products = productService.getAllProducts();
        assertEquals(3, products.size());
    }

    @Test
    void getAllProducts_shouldReturnEmptyListWhenNoProducts() {
        List<Product> products = productService.getAllProducts();
        assertTrue(products.isEmpty());
    }

    @Test
    void createProduct_shouldAddProductAndReturnIt() {
        Product product = new Product("Test", "Desc", new BigDecimal("10.00"), "Cat");
        Product created = productService.createProduct(product);

        assertNotNull(created.getId());
        assertEquals("Test", created.getName());
        assertEquals(1, productService.getAllProducts().size());
    }

    @Test
    void getProductById_shouldReturnProductWhenExists() {
        Product product = new Product("Test", "Desc", new BigDecimal("10.00"), "Cat");
        productService.createProduct(product);

        Optional<Product> found = productService.getProductById(product.getId());

        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getName());
    }

    @Test
    void getProductById_shouldReturnEmptyWhenNotExists() {
        Optional<Product> found = productService.getProductById("non-existent-id");
        assertTrue(found.isEmpty());
    }

    @Test
    void updateProduct_shouldUpdateExistingProduct() {
        Product product = new Product("Original", "Desc", new BigDecimal("10.00"), "Cat");
        productService.createProduct(product);

        Product updated = new Product("Updated", "New Desc", new BigDecimal("20.00"), "NewCat");
        Optional<Product> result = productService.updateProduct(product.getId(), updated);

        assertTrue(result.isPresent());
        assertEquals("Updated", result.get().getName());
        assertEquals(new BigDecimal("20.00"), result.get().getPrice());
        assertEquals(product.getId(), result.get().getId());
    }

    @Test
    void updateProduct_shouldReturnEmptyWhenProductNotExists() {
        Product updated = new Product("Updated", "Desc", new BigDecimal("20.00"), "Cat");
        Optional<Product> result = productService.updateProduct("non-existent-id", updated);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteProduct_shouldReturnTrueWhenProductExists() {
        Product product = new Product("Test", "Desc", new BigDecimal("10.00"), "Cat");
        productService.createProduct(product);

        assertTrue(productService.deleteProduct(product.getId()));
        assertTrue(productService.getAllProducts().isEmpty());
    }

    @Test
    void deleteProduct_shouldReturnFalseWhenProductNotExists() {
        assertFalse(productService.deleteProduct("non-existent-id"));
    }

    @Test
    void createMultipleProducts_shouldReturnAll() {
        productService.createProduct(new Product("A", "Desc A", new BigDecimal("1.00"), "Cat"));
        productService.createProduct(new Product("B", "Desc B", new BigDecimal("2.00"), "Cat"));
        productService.createProduct(new Product("C", "Desc C", new BigDecimal("3.00"), "Cat"));

        assertEquals(3, productService.getAllProducts().size());
    }
}

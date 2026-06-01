package com.boschtech.productservice.service;

import com.boschtech.productservice.model.Product;
import com.boschtech.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);
    }

    @Test
    void init_shouldSeedThreeProductsWhenEmpty() {
        when(productRepository.count()).thenReturn(0L);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.init();

        verify(productRepository, times(3)).save(any(Product.class));
    }

    @Test
    void init_shouldNotSeedWhenProductsExist() {
        when(productRepository.count()).thenReturn(3L);

        productService.init();

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getAllProducts_shouldReturnEmptyListWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<Product> products = productService.getAllProducts();
        assertTrue(products.isEmpty());
    }

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        Product product = new Product("Test", "Desc", new BigDecimal("10.00"), "Cat");
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product created = productService.createProduct(product);

        assertEquals("Test", created.getName());
        verify(productRepository).save(product);
    }

    @Test
    void getProductById_shouldReturnProductWhenExists() {
        Product product = new Product("Test", "Desc", new BigDecimal("10.00"), "Cat");
        product.setId("test-id");
        when(productRepository.findById("test-id")).thenReturn(Optional.of(product));

        Optional<Product> found = productService.getProductById("test-id");

        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getName());
    }

    @Test
    void getProductById_shouldReturnEmptyWhenNotExists() {
        when(productRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        Optional<Product> found = productService.getProductById("non-existent-id");
        assertTrue(found.isEmpty());
    }

    @Test
    void updateProduct_shouldUpdateExistingProduct() {
        Product updated = new Product("Updated", "New Desc", new BigDecimal("20.00"), "NewCat");
        when(productRepository.existsById("existing-id")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Product> result = productService.updateProduct("existing-id", updated);

        assertTrue(result.isPresent());
        assertEquals("Updated", result.get().getName());
        assertEquals(new BigDecimal("20.00"), result.get().getPrice());
        assertEquals("existing-id", result.get().getId());
    }

    @Test
    void updateProduct_shouldReturnEmptyWhenProductNotExists() {
        when(productRepository.existsById("non-existent-id")).thenReturn(false);

        Product updated = new Product("Updated", "Desc", new BigDecimal("20.00"), "Cat");
        Optional<Product> result = productService.updateProduct("non-existent-id", updated);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteProduct_shouldReturnTrueWhenProductExists() {
        when(productRepository.existsById("existing-id")).thenReturn(true);

        assertTrue(productService.deleteProduct("existing-id"));
        verify(productRepository).deleteById("existing-id");
    }

    @Test
    void deleteProduct_shouldReturnFalseWhenProductNotExists() {
        when(productRepository.existsById("non-existent-id")).thenReturn(false);

        assertFalse(productService.deleteProduct("non-existent-id"));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    void getAllProducts_shouldReturnAllProducts() {
        List<Product> products = List.of(
                new Product("A", "Desc A", new BigDecimal("1.00"), "Cat"),
                new Product("B", "Desc B", new BigDecimal("2.00"), "Cat"),
                new Product("C", "Desc C", new BigDecimal("3.00"), "Cat")
        );
        when(productRepository.findAll()).thenReturn(products);

        assertEquals(3, productService.getAllProducts().size());
    }
}

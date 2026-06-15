package com.boschtech.productservice.controller;

import com.boschtech.productservice.client.OrderClient;
import com.boschtech.productservice.model.OrderDto;
import com.boschtech.productservice.model.Product;
import com.boschtech.productservice.security.SecurityConfig;
import com.boschtech.productservice.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    private static final String API_KEY = "test-api-key-for-tests";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private OrderClient orderClient;

    @Autowired
    private ObjectMapper objectMapper;

    private Product createTestProduct() {
        Product product = new Product("Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics");
        product.setId("test-id-123");
        return product;
    }

    // --- GET /api/products ---

    @Test
    void getAllProducts_shouldReturnListOfProducts() throws Exception {
        Product product = createTestProduct();
        when(productService.getAllProducts()).thenReturn(List.of(product));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].price").value(999.99))
                .andExpect(jsonPath("$[0].category").value("Electronics"));
    }

    @Test
    void getAllProducts_shouldReturnEmptyList() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- GET /api/products/{id} ---

    @Test
    void getProductById_shouldReturnProductWhenExists() throws Exception {
        Product product = createTestProduct();
        when(productService.getProductById("test-id-123")).thenReturn(Optional.of(product));

        mockMvc.perform(get("/api/products/test-id-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.id").value("test-id-123"))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void getProductById_shouldReturn404WhenNotExists() throws Exception {
        when(productService.getProductById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/non-existent"))
                .andExpect(status().isNotFound());
    }

    // --- POST /api/products ---

    @Test
    void createProduct_shouldReturn201WithCreatedProduct() throws Exception {
        Product product = createTestProduct();
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        String json = objectMapper.writeValueAsString(product);

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void createProduct_shouldPersistOutOfStockStatus() throws Exception {
        Product product = createTestProduct();
        product.setInStock(false);
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        String json = objectMapper.writeValueAsString(product);

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inStock").value(false));
    }

    @Test
    void createProduct_shouldReturn401WhenNoApiKey() throws Exception {
        Product product = createTestProduct();
        String json = objectMapper.writeValueAsString(product);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_shouldReturn401WhenInvalidApiKey() throws Exception {
        Product product = createTestProduct();
        String json = objectMapper.writeValueAsString(product);

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createProduct_shouldReturn400WhenNameIsBlank() throws Exception {
        Product product = new Product();
        product.setName("");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory("Cat");

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_shouldReturn400WhenPriceIsNull() throws Exception {
        Product product = new Product();
        product.setName("Test");
        product.setCategory("Cat");
        product.setPrice(null);

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_shouldReturn400WhenPriceIsNegative() throws Exception {
        Product product = new Product();
        product.setName("Test");
        product.setCategory("Cat");
        product.setPrice(new BigDecimal("-5.00"));

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProduct_shouldReturn400WhenCategoryIsBlank() throws Exception {
        Product product = new Product();
        product.setName("Test");
        product.setPrice(new BigDecimal("10.00"));
        product.setCategory("");

        mockMvc.perform(post("/api/products")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/products/{id} ---

    @Test
    void updateProduct_shouldReturnUpdatedProduct() throws Exception {
        Product product = createTestProduct();
        product.setName("Updated Laptop");
        when(productService.updateProduct(eq("test-id-123"), any(Product.class))).thenReturn(Optional.of(product));

        mockMvc.perform(put("/api/products/test-id-123")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"));
    }

    @Test
    void updateProduct_shouldReturn401WhenNoApiKey() throws Exception {
        Product product = createTestProduct();

        mockMvc.perform(put("/api/products/test-id-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProduct_shouldReturn404WhenNotExists() throws Exception {
        Product product = createTestProduct();
        when(productService.updateProduct(eq("non-existent"), any(Product.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/products/non-existent")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_shouldReturn400WhenInvalidBody() throws Exception {
        Product product = new Product();
        product.setName("");
        product.setPrice(null);
        product.setCategory("");

        mockMvc.perform(put("/api/products/test-id-123")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /api/products/{id} ---

    @Test
    void deleteProduct_shouldReturn204WhenDeleted() throws Exception {
        when(productService.deleteProduct("test-id-123")).thenReturn(true);

        mockMvc.perform(delete("/api/products/test-id-123")
                        .header("X-API-Key", API_KEY))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_shouldReturn401WhenNoApiKey() throws Exception {
        mockMvc.perform(delete("/api/products/test-id-123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteProduct_shouldReturn404WhenNotExists() throws Exception {
        when(productService.deleteProduct("non-existent")).thenReturn(false);

        mockMvc.perform(delete("/api/products/non-existent")
                        .header("X-API-Key", API_KEY))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/products/{id}/orders ---

    private OrderDto createOrderDto(String id, String status, int quantity, String total) {
        OrderDto order = new OrderDto();
        order.setId(id);
        order.setProductId("test-id-123");
        order.setProductName("Laptop");
        order.setQuantity(quantity);
        order.setTotalPrice(new BigDecimal(total));
        order.setStatus(status);
        order.setCreatedAt("2025-01-15T10:00:00Z");
        return order;
    }

    @Test
    void getProductOrders_shouldReturnOrdersEnvelopeWhenProductExists() throws Exception {
        Product product = createTestProduct();
        when(productService.getProductById("test-id-123")).thenReturn(Optional.of(product));
        when(orderClient.getOrdersByProductId("test-id-123")).thenReturn(List.of(
                createOrderDto("order-001", "CONFIRMED", 2, "1999.98"),
                createOrderDto("order-002", "PENDING", 1, "999.99")
        ));

        mockMvc.perform(get("/api/products/test-id-123/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("test-id-123"))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.orderCount").value(2))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.orders[0].id").value("order-001"))
                .andExpect(jsonPath("$.orders[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orders[1].id").value("order-002"))
                .andExpect(jsonPath("$.orders[1].status").value("PENDING"));
    }

    @Test
    void getProductOrders_shouldReturnEmptyOrdersWhenProductHasNoOrders() throws Exception {
        Product product = createTestProduct();
        when(productService.getProductById("test-id-123")).thenReturn(Optional.of(product));
        when(orderClient.getOrdersByProductId("test-id-123")).thenReturn(List.of());

        mockMvc.perform(get("/api/products/test-id-123/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("test-id-123"))
                .andExpect(jsonPath("$.productName").value("Laptop"))
                .andExpect(jsonPath("$.orderCount").value(0))
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.orders.length()").value(0));
    }

    @Test
    void getProductOrders_shouldReturn404WhenProductDoesNotExist() throws Exception {
        when(productService.getProductById("non-existent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/non-existent/orders"))
                .andExpect(status().isNotFound());
    }
}

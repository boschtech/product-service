package com.boschtech.productservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.boschtech.productservice.client.OrderClient;
import com.boschtech.productservice.model.OrderDto;
import com.boschtech.productservice.model.Product;
import com.boschtech.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Provider verification test: product-service verifies it satisfies
 * the contract defined by the micro-frontend (the consumer).
 * Loads pact from micro-frontend's pacts directory.
 *
 * Security is disabled for pact verification — contracts test
 * request/response shape, not authentication.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@Provider("product-service")
@PactFolder("../micro-frontend/pacts")
class MicroFrontendProductProviderPactTest {

    /** Permits all requests so pact verification is not blocked by API-key auth. */
    @TestConfiguration
    static class PactSecurityOverride {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ProductService productService;

    @MockBean
    private OrderClient orderClient;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("products exist")
    void setupProductsExist() {
        Product product = new Product("Widget Alpha", "A premium widget for all your needs",
                new BigDecimal("29.99"), "Widgets");
        product.setId("prod-001");
        productService.createProduct(product);
    }

    @State("product prod-001 exists")
    void setupProductExists() {
        Product product = new Product("Widget Alpha", "A premium widget for all your needs",
                new BigDecimal("29.99"), "Widgets");
        product.setId("prod-001");
        productService.createProduct(product);
    }

    @State("the product service is available")
    void setupServiceAvailable() {
        // Service is running — no specific data setup needed
    }

    @State("product prod-001 has orders")
    void setupProductHasOrders() {
        // Ensure product exists
        Product product = new Product("Widget Alpha", "A premium widget for all your needs",
                new BigDecimal("29.99"), "Widgets");
        product.setId("prod-001");
        productService.createProduct(product);

        // Mock the OrderClient to return orders for this product
        OrderDto order = new OrderDto();
        order.setId("order-001");
        order.setProductId("prod-001");
        order.setProductName("Widget Alpha");
        order.setQuantity(2);
        order.setTotalPrice(new BigDecimal("59.98"));
        order.setStatus("CONFIRMED");
        order.setCreatedAt("2025-01-15T10:00:00Z");

        when(orderClient.getOrdersByProductId(eq("prod-001")))
                .thenReturn(List.of(order));
    }
}

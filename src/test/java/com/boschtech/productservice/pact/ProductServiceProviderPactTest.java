package com.boschtech.productservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.boschtech.productservice.model.Product;
import com.boschtech.productservice.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;

/**
 * Provider verification test: product-service verifies it satisfies
 * the contract defined by order-service (the consumer).
 * Loads pact from order-service's target/pacts directory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("product_service")
@PactFolder("../order-service/target/pacts")
class ProductServiceProviderPactTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductService productService;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("a product with ID product-001 exists")
    void setupProductExists() {
        Product product = new Product("Wireless Keyboard", "Bluetooth mechanical keyboard",
                new BigDecimal("79.99"), "Electronics");
        product.setId("product-001");
        productService.createProduct(product);
    }

    @State("no product with ID missing-product exists")
    void setupProductNotFound() {
        // Ensure no product with this ID exists — nothing to do
    }
}

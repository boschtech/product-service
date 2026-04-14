package com.boschtech.productservice.component.tests;

import com.boschtech.productservice.model.Product;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Component tests that spin up the full Spring Boot application on a random port
 * and interact with it over HTTP using WebTestClient — simulating how a
 * microfrontend would call the API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductApiComponentTest {

    @Autowired
    private WebTestClient webTestClient;

    // Stored across tests via static field (tests run in order)
    private static String createdProductId;

    // ─── GET /api/products ──────────────────────────────────────────

    @Test
    @Order(1)
    void shouldReturnSeededProductsOnStartup() {
        webTestClient.get()
                .uri("/api/products")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Product.class)
                .hasSize(3);
    }

    // ─── POST /api/products ─────────────────────────────────────────

    @Test
    @Order(2)
    void shouldCreateNewProduct() {
        Product newProduct = new Product("Monitor", "4K Ultra HD monitor", new BigDecimal("399.99"), "Electronics");

        webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(newProduct)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Product.class)
                .value(product -> {
                    assertNotNull(product.getId());
                    assertEquals("Monitor", product.getName());
                    assertEquals("4K Ultra HD monitor", product.getDescription());
                    assertEquals(0, new BigDecimal("399.99").compareTo(product.getPrice()));
                    assertEquals("Electronics", product.getCategory());
                    assertTrue(product.isInStock());
                    // Store the ID for subsequent tests
                    createdProductId = product.getId();
                });
    }

    @Test
    @Order(3)
    void shouldHaveFourProductsAfterCreation() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .hasSize(4);
    }

    // ─── POST validation (bad requests) ─────────────────────────────

    @Test
    @Order(4)
    void shouldRejectProductWithBlankName() {
        String json = """
                {
                    "name": "",
                    "description": "No name",
                    "price": 10.00,
                    "category": "Test"
                }
                """;

        webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(5)
    void shouldRejectProductWithNegativePrice() {
        String json = """
                {
                    "name": "Bad Product",
                    "description": "Negative price",
                    "price": -5.00,
                    "category": "Test"
                }
                """;

        webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Order(6)
    void shouldRejectProductWithMissingCategory() {
        String json = """
                {
                    "name": "No Category",
                    "description": "Missing category",
                    "price": 10.00,
                    "category": ""
                }
                """;

        webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ─── GET /api/products/{id} ─────────────────────────────────────

    @Test
    @Order(7)
    void shouldGetProductById() {
        assertNotNull(createdProductId, "Product ID should have been set by create test");

        webTestClient.get()
                .uri("/api/products/{id}", createdProductId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .value(product -> {
                    assertEquals(createdProductId, product.getId());
                    assertEquals("Monitor", product.getName());
                });
    }

    @Test
    @Order(8)
    void shouldReturn404ForNonExistentProduct() {
        webTestClient.get()
                .uri("/api/products/{id}", "non-existent-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ─── PUT /api/products/{id} ─────────────────────────────────────

    @Test
    @Order(9)
    void shouldUpdateExistingProduct() {
        assertNotNull(createdProductId);

        Product updated = new Product("Monitor Pro", "5K Ultra HD monitor", new BigDecimal("599.99"), "Electronics");

        webTestClient.put()
                .uri("/api/products/{id}", createdProductId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updated)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .value(product -> {
                    assertEquals(createdProductId, product.getId());
                    assertEquals("Monitor Pro", product.getName());
                    assertEquals("5K Ultra HD monitor", product.getDescription());
                    assertEquals(0, new BigDecimal("599.99").compareTo(product.getPrice()));
                });
    }

    @Test
    @Order(10)
    void shouldReturn404WhenUpdatingNonExistentProduct() {
        Product product = new Product("Ghost", "Does not exist", new BigDecimal("1.00"), "None");

        webTestClient.put()
                .uri("/api/products/{id}", "non-existent-id")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(product)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(11)
    void shouldRejectUpdateWithInvalidBody() {
        String json = """
                {
                    "name": "",
                    "price": -1,
                    "category": ""
                }
                """;

        webTestClient.put()
                .uri("/api/products/{id}", createdProductId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(json)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ─── DELETE /api/products/{id} ──────────────────────────────────

    @Test
    @Order(12)
    void shouldDeleteExistingProduct() {
        assertNotNull(createdProductId);

        webTestClient.delete()
                .uri("/api/products/{id}", createdProductId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @Order(13)
    void shouldReturn404WhenDeletingAlreadyDeletedProduct() {
        webTestClient.delete()
                .uri("/api/products/{id}", createdProductId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @Order(14)
    void shouldHaveThreeProductsAfterDeletion() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Product.class)
                .hasSize(3);
    }

    // ─── Actuator health (used by microfrontend orchestrators) ──────

    @Test
    @Order(15)
    void shouldExposeHealthEndpoint() {
        webTestClient.get()
                .uri("/actuator/health")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }

    // ─── Content-Type negotiation ───────────────────────────────────

    @Test
    @Order(16)
    void shouldReturnJsonContentType() {
        webTestClient.get()
                .uri("/api/products")
                .exchange()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }

    // ─── Full CRUD lifecycle in a single test ───────────────────────

    @Test
    @Order(17)
    void shouldSupportFullCrudLifecycle() {
        // CREATE
        Product created = webTestClient.post()
                .uri("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Product("Headphones", "Noise cancelling", new BigDecimal("249.99"), "Audio"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Product.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(created);
        String id = created.getId();

        // READ
        webTestClient.get()
                .uri("/api/products/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .value(p -> assertEquals("Headphones", p.getName()));

        // UPDATE
        webTestClient.put()
                .uri("/api/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new Product("Headphones Pro", "Active noise cancelling", new BigDecimal("349.99"), "Audio"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Product.class)
                .value(p -> assertEquals("Headphones Pro", p.getName()));

        // DELETE
        webTestClient.delete()
                .uri("/api/products/{id}", id)
                .exchange()
                .expectStatus().isNoContent();

        // VERIFY GONE
        webTestClient.get()
                .uri("/api/products/{id}", id)
                .exchange()
                .expectStatus().isNotFound();
    }
}

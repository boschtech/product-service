package com.boschtech.productservice.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer contract test: product-service defines what it expects from order-service.
 * Generates pact file: product_service-order_service.json
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "order_service")
class ProductServiceConsumerPactTest {

    @Pact(provider = "order_service", consumer = "product_service")
    V4Pact getOrdersByProductIdPact(PactDslWithProvider builder) {
        return builder
                .given("orders exist for product product-001")
                .uponReceiving("a request to get orders by product ID")
                .path("/api/orders/product/product-001")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(PactDslJsonArray.arrayMinLike(1)
                        .stringType("id", "order-001")
                        .stringType("productId", "product-001")
                        .stringType("productName", "Wireless Keyboard")
                        .integerType("quantity", 2)
                        .decimalType("totalPrice", 159.98)
                        .stringType("status", "CONFIRMED")
                        .stringType("createdAt", "2026-04-14T10:00:00")
                        .closeObject())
                .toPact(V4Pact.class);
    }

    @Pact(provider = "order_service", consumer = "product_service")
    V4Pact getEmptyOrdersForProductPact(PactDslWithProvider builder) {
        return builder
                .given("no orders exist for product no-orders-product")
                .uponReceiving("a request to get orders for a product with no orders")
                .path("/api/orders/product/no-orders-product")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(PactDslJsonArray.arrayMinLike(0))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getOrdersByProductIdPact")
    void testGetOrdersByProductId(MockServer mockServer) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.getUrl())
                .build();

        List<Map> response = restClient.get()
                .uri("/api/orders/product/product-001")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map>>() {});

        assertThat(response).isNotNull().isNotEmpty();
        assertThat(response.get(0).get("productId")).isEqualTo("product-001");
        assertThat(response.get(0).get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    @PactTestFor(pactMethod = "getEmptyOrdersForProductPact")
    void testGetEmptyOrdersForProduct(MockServer mockServer) {
        RestClient restClient = RestClient.builder()
                .baseUrl(mockServer.getUrl())
                .build();

        List<Map> response = restClient.get()
                .uri("/api/orders/product/no-orders-product")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map>>() {});

        assertThat(response).isNotNull();
    }
}

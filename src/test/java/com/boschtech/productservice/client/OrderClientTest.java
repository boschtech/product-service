package com.boschtech.productservice.client;

import com.boschtech.productservice.model.OrderDto;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link OrderClient} against an in-process HTTP server so we can
 * exercise the full {@code RestClient} pipeline (baseUrl, URI template,
 * {@code ParameterizedTypeReference<List<OrderDto>>} deserialization)
 * without any network dependency.
 */
class OrderClientTest {

    private HttpServer server;
    private int port;
    private AtomicReference<Integer> nextStatus;
    private AtomicReference<String> nextBody;

    @BeforeEach
    void startServer() throws IOException {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        server = HttpServer.create(new InetSocketAddress(loopback, 0), 0);
        port = server.getAddress().getPort();
        nextStatus = new AtomicReference<>(200);
        nextBody = new AtomicReference<>("[]");

        server.createContext("/api/orders/product/", exchange -> {
            byte[] bytes = nextBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(nextStatus.get(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.close();
            }
        });

        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private OrderClient clientFor(String baseUrl) {
        return new OrderClient(baseUrl);
    }

    private String baseUrl() {
        return "http://" + InetAddress.getLoopbackAddress().getHostAddress() + ":" + port;
    }

    @Test
    void getOrdersByProductId_returnsOrders_on200() {
        nextStatus.set(200);
        nextBody.set(
                "["
                        + "{"
                        + "\"id\":\"order-001\","
                        + "\"productId\":\"prod-001\","
                        + "\"productName\":\"Widget Alpha\","
                        + "\"quantity\":2,"
                        + "\"totalPrice\":59.98,"
                        + "\"status\":\"CONFIRMED\","
                        + "\"createdAt\":\"2025-01-15T10:00:00Z\""
                        + "},"
                        + "{"
                        + "\"id\":\"order-002\","
                        + "\"productId\":\"prod-001\","
                        + "\"productName\":\"Widget Alpha\","
                        + "\"quantity\":1,"
                        + "\"totalPrice\":29.99,"
                        + "\"status\":\"PENDING\","
                        + "\"createdAt\":\"2025-01-16T11:00:00Z\""
                        + "}"
                        + "]"
        );

        List<OrderDto> orders = clientFor(baseUrl()).getOrdersByProductId("prod-001");

        assertEquals(2, orders.size());

        OrderDto first = orders.get(0);
        assertEquals("order-001", first.getId());
        assertEquals("prod-001", first.getProductId());
        assertEquals("Widget Alpha", first.getProductName());
        assertEquals(2, first.getQuantity());
        assertNotNull(first.getTotalPrice());
        assertEquals(0, first.getTotalPrice().compareTo(new java.math.BigDecimal("59.98")));
        assertEquals("CONFIRMED", first.getStatus());
        assertEquals("2025-01-15T10:00:00Z", first.getCreatedAt());

        OrderDto second = orders.get(1);
        assertEquals("order-002", second.getId());
        assertEquals("PENDING", second.getStatus());
    }

    @Test
    void getOrdersByProductId_returnsEmptyList_on200WithEmptyArray() {
        nextStatus.set(200);
        nextBody.set("[]");

        List<OrderDto> orders = clientFor(baseUrl()).getOrdersByProductId("prod-001");

        assertTrue(orders.isEmpty());
    }

    @Test
    void getOrdersByProductId_returnsEmptyList_on404() {
        nextStatus.set(404);
        nextBody.set("");

        List<OrderDto> orders = clientFor(baseUrl()).getOrdersByProductId("missing");

        assertTrue(orders.isEmpty());
    }

    @Test
    void getOrdersByProductId_returnsEmptyList_on500() {
        nextStatus.set(500);
        nextBody.set("");

        List<OrderDto> orders = clientFor(baseUrl()).getOrdersByProductId("boom");

        assertTrue(orders.isEmpty());
    }

    @Test
    void getOrdersByProductId_returnsEmptyList_whenBodyIsNullWith200() {
        // 200 OK but no JSON body -> RestClient returns null list -> fall
        // through to the Collections.emptyList() guard.
        nextStatus.set(200);
        nextBody.set("");

        List<OrderDto> orders = clientFor(baseUrl()).getOrdersByProductId("ghost");

        assertTrue(orders.isEmpty());
    }

    @Test
    void getOrdersByProductId_returnsEmptyList_whenHostUnreachable() {
        // Kill the server before the call so the HTTP request fails with a
        // ResourceAccessException (subtype of RestClientException).
        server.stop(0);

        List<OrderDto> orders = clientFor(baseUrl()).getOrdersByProductId("any");

        assertTrue(orders.isEmpty());
    }
}

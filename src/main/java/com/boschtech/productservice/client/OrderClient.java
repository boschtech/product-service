package com.boschtech.productservice.client;

import com.boschtech.productservice.model.OrderDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class OrderClient {

    private final RestClient restClient;

    @Autowired
    public OrderClient(@Value("${app.order-service.url}") String orderServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(orderServiceUrl)
                .build();
    }

    public List<OrderDto> getOrdersByProductId(String productId) {
        try {
            List<OrderDto> orders = restClient.get()
                    .uri("/api/orders/product/{productId}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<OrderDto>>() {});
            return orders != null ? orders : Collections.emptyList();
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }
}

package com.boschtech.productservice.controller;

import com.boschtech.productservice.client.OrderClient;
import com.boschtech.productservice.model.OrderDto;
import com.boschtech.productservice.model.Product;
import com.boschtech.productservice.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final OrderClient orderClient;

    public ProductController(ProductService productService, OrderClient orderClient) {
        this.productService = productService;
        this.orderClient = orderClient;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(name = "search", required = false) String search) {
        return ResponseEntity.ok(productService.getAllProducts(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product created = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id,
                                                  @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        if (productService.deleteProduct(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/orders")
    public ResponseEntity<?> getProductOrders(@PathVariable String id) {
        return productService.getProductById(id)
                .map(product -> {
                    List<OrderDto> orders = orderClient.getOrdersByProductId(id);
                    return ResponseEntity.ok(Map.of(
                            "productId", id,
                            "productName", product.getName(),
                            "orderCount", orders.size(),
                            "orders", orders
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

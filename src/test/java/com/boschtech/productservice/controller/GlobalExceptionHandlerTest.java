package com.boschtech.productservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrors_shouldReturnBadRequestWithFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "product");
        bindingResult.addError(new FieldError("product", "name", "Product name is required"));
        bindingResult.addError(new FieldError("product", "price", "Price must be positive"));

        var ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("error"));

        @SuppressWarnings("unchecked")
        List<String> details = (List<String>) response.getBody().get("details");
        assertEquals(2, details.size());
        assertTrue(details.stream().anyMatch(d -> d.contains("name")));
        assertTrue(details.stream().anyMatch(d -> d.contains("price")));
    }

    @Test
    void handleMalformedJson_shouldReturnBadRequest() {
        var ex = new HttpMessageNotReadableException("Malformed JSON");

        ResponseEntity<Map<String, String>> response = handler.handleMalformedJson(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Malformed request body", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequestWithMessage() {
        var ex = new IllegalArgumentException("Product not found: abc-123");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Product not found: abc-123", response.getBody().get("error"));
    }

    @Test
    void handleGeneric_shouldReturnInternalServerError() {
        var ex = new RuntimeException("Something went wrong");

        ResponseEntity<Map<String, String>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Internal server error", response.getBody().get("error"));
    }
}

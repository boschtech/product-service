package com.boschtech.productservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void defaultConstructor_shouldGenerateIdAndSetInStock() {
        Product product = new Product();
        assertNotNull(product.getId());
        assertFalse(product.getId().isEmpty());
        assertTrue(product.isInStock());
    }

    @Test
    void parameterizedConstructor_shouldSetAllFields() {
        Product product = new Product("Keyboard", "Mechanical", new BigDecimal("79.99"), "Electronics");

        assertNotNull(product.getId());
        assertEquals("Keyboard", product.getName());
        assertEquals("Mechanical", product.getDescription());
        assertEquals(new BigDecimal("79.99"), product.getPrice());
        assertEquals("Electronics", product.getCategory());
        assertTrue(product.isInStock());
    }

    @Test
    void setters_shouldUpdateFields() {
        Product product = new Product();

        product.setId("custom-id");
        product.setName("Mouse");
        product.setDescription("Wireless mouse");
        product.setPrice(new BigDecimal("29.99"));
        product.setCategory("Peripherals");
        product.setInStock(false);

        assertEquals("custom-id", product.getId());
        assertEquals("Mouse", product.getName());
        assertEquals("Wireless mouse", product.getDescription());
        assertEquals(new BigDecimal("29.99"), product.getPrice());
        assertEquals("Peripherals", product.getCategory());
        assertFalse(product.isInStock());
    }

    @Test
    void twoProducts_shouldHaveDifferentIds() {
        Product p1 = new Product();
        Product p2 = new Product();
        assertNotEquals(p1.getId(), p2.getId());
    }
}

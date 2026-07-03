package com.boschtech.productservice.repository;

import com.boschtech.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Case-insensitive search matching products whose name, description or
     * category contains the given term.
     */
    @Query("SELECT p FROM Product p WHERE "
            + "LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) "
            + "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%')) "
            + "OR LOWER(p.category) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Product> search(@Param("term") String term);
}

package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    @Query("""
            select p from Product p
            where lower(p.name) like lower(concat('%', :search, '%'))
               or lower(p.sku) like lower(concat('%', :search, '%'))
            """)
    Page<Product> searchByTerm(@Param("search") String search, Pageable pageable);

    @Query("""
            select p from Product p
            where p.categoryId = :categoryId
              and (
                lower(p.name) like lower(concat('%', :search, '%'))
                or lower(p.sku) like lower(concat('%', :search, '%'))
              )
            """)
    Page<Product> searchByCategoryIdAndTerm(
            @Param("categoryId") UUID categoryId,
            @Param("search") String search,
            Pageable pageable
    );
}

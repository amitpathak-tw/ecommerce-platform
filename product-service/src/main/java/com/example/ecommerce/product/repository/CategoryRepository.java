package com.example.ecommerce.product.repository;
import com.example.ecommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CategoryRepository extends JpaRepository<Category, UUID> {}

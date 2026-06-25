package com.example.ecommerce.product.service;
import com.example.ecommerce.product.dto.response.CategoryResponse;
import com.example.ecommerce.product.entity.Category;
import com.example.ecommerce.product.exception.CategoryNotFoundException;
import com.example.ecommerce.product.mapper.ProductMapper;
import com.example.ecommerce.product.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import java.util.*;
@Service public class CategoryService {
 private final CategoryRepository repository; private final ProductMapper mapper;
 public CategoryService(CategoryRepository repository,ProductMapper mapper){this.repository=repository;this.mapper=mapper;}
 public List<CategoryResponse> getAll(){return repository.findAll().stream().map(mapper::toResponse).toList();}
 public Category require(UUID id){return repository.findById(id).orElseThrow(()->new CategoryNotFoundException(id));}
}

package com.example.ecommerce.product.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "products")
public class Product {
    @Id private UUID id;
    @Column(nullable=false,length=150) private String name;
    @Column(columnDefinition="TEXT") private String description;
    @Column(nullable=false,unique=true,length=100) private String sku;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
    @Column(nullable=false) private UUID categoryId;
    @Column(nullable=false) private boolean active;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected Product() {}
    public Product(UUID id,String name,String description,String sku,BigDecimal price,UUID categoryId,boolean active,Instant createdAt,Instant updatedAt){this.id=id;this.name=name;this.description=description;this.sku=sku;this.price=price;this.categoryId=categoryId;this.active=active;this.createdAt=createdAt;this.updatedAt=updatedAt;}
    public void update(String name,String description,String sku,BigDecimal price,UUID categoryId,boolean active,Instant updatedAt){this.name=name;this.description=description;this.sku=sku;this.price=price;this.categoryId=categoryId;this.active=active;this.updatedAt=updatedAt;}
    public UUID getId(){return id;} public String getName(){return name;} public String getDescription(){return description;} public String getSku(){return sku;} public BigDecimal getPrice(){return price;} public UUID getCategoryId(){return categoryId;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}

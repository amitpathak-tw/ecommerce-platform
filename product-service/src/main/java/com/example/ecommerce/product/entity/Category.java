package com.example.ecommerce.product.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "categories")
public class Category {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 100) private String name;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    protected Category() {}
    public Category(UUID id, String name, String description, Instant createdAt, Instant updatedAt) { this.id=id; this.name=name; this.description=description; this.createdAt=createdAt; this.updatedAt=updatedAt; }
    public UUID getId(){return id;} public String getName(){return name;} public String getDescription(){return description;}
}

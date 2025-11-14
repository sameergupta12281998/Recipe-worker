package com.example.recipe_worker.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.recipe_worker.entity.ImageEntity;

public interface ImageRepository extends JpaRepository<ImageEntity, UUID> {
    
}

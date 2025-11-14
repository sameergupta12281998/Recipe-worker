package com.example.recipe_worker.repository;

import com.example.recipe_worker.entity.Chef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChefRepository extends JpaRepository<Chef, UUID> {
    Optional<Chef> findByEmail(String email);
    Optional<Chef> findByHandle(String handle);
}

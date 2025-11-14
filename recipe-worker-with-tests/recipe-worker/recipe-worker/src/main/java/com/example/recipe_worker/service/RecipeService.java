package com.example.recipe_worker.service;

import com.example.recipe_worker.dto.PagedResponse;
import com.example.recipe_worker.dto.RecipeCreateRequest;
import com.example.recipe_worker.dto.RecipeResponse;
import com.example.recipe_worker.dto.RecipeUpdateRequest;
import com.example.recipe_worker.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface RecipeService {
    RecipeResponse createRecipe(RecipeCreateRequest req, String chefEmail);
    Optional<RecipeResponse> getById(UUID id);
    RecipeResponse updateRecipe(UUID id, RecipeUpdateRequest update, String actorEmail);
    void deleteRecipe(UUID id, String actorEmail);
    PagedResponse<RecipeResponse> searchRecipes(
        String q,
        String publishedFrom,
        String publishedTo,
        String chefId,    
        String chefHandle,
        int page,
        int pageSize,
        String sortBy,
        String sortDir
    );



}

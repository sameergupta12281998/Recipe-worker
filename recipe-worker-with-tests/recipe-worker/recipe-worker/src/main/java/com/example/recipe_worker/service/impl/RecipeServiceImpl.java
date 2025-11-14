package com.example.recipe_worker.service.impl;

import com.example.recipe_worker.dto.*;
import com.example.recipe_worker.entity.Chef;
import com.example.recipe_worker.entity.ImageEntity;
import com.example.recipe_worker.entity.Recipe;
import com.example.recipe_worker.entity.User;
import com.example.recipe_worker.repository.ChefRepository;
import com.example.recipe_worker.repository.RecipeRepository;
import com.example.recipe_worker.service.FileStorageService;
import com.example.recipe_worker.service.RecipeService;
import com.example.recipe_worker.spec.RecipeSpecifications;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecipeServiceImpl implements RecipeService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private final RecipeRepository recipeRepository;
    private final ChefRepository chefRepository;
    private final FileStorageService fileStorageService;

    public RecipeServiceImpl(RecipeRepository recipeRepository, ChefRepository chefRepository,
                             FileStorageService fileStorageService) {
        this.recipeRepository = recipeRepository;
        this.chefRepository = chefRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public RecipeResponse createRecipe(RecipeCreateRequest req, String chefEmail) {
        Recipe r = new Recipe();
        r.setId(UUID.randomUUID());
        r.setTitle(req.getTitle());
        r.setSummary(req.getSummary());
        r.setIngredients(req.getIngredients());
        r.setSteps(req.getSteps());
        r.setLabels(req.getLabels());
        r.setPublished(req.isPublished());
        if (req.isPublished()) r.setPublishedAt(Instant.now());

        // find or create chef by email
        Chef chef = null;
        if (chefEmail != null) {
            chef = chefRepository.findByEmail(chefEmail).orElseGet(() -> {
                Chef c = new Chef();
                c.setEmail(chefEmail);
                c.setHandle(chefEmail.split("@")[0]);
                return chefRepository.save(c);
            });
        }
        r.setChef(chef);

        // images: RecipeCreateRequest.images contains ImageUploadDto objects.
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            List<ImageEntity> imgs = new ArrayList<>();
            for (ImageUploadDto im : req.getImages()) {
                ImageEntity e = new ImageEntity();
                // if the image DTO has dataBase64 filled we decode and store; else if filename contains stored path, just use it
                if (im.getDataBase64() != null && !im.getDataBase64().isBlank()) {
                    byte[] data = Base64.getDecoder().decode(im.getDataBase64());
                    String storedPath = fileStorageService.store(data, im.getFilename());
                    e.setPath(storedPath);
                } else {
                    // assume filename contains stored path (we used that in controller)
                    e.setPath(im.getFilename());
                }
                e.setFilename(im.getFilename());
                e.setSizeLabel(null);
                r.addImage(e);
            }
        }

        Recipe saved = recipeRepository.save(r);
        // map to RecipeResponse
        return toResponse(saved);
    }

    @Override
    public Optional<RecipeResponse> getById(UUID id) {
        return recipeRepository.findById(id).map(this::toResponse);
    }

    @Override
    public RecipeResponse updateRecipe(UUID id, RecipeUpdateRequest update, String actorEmail) {
        Recipe r = recipeRepository.findById(id).orElseThrow(NoSuchElementException::new);

        // authorization: only owner chef or admin (you need to expand with UserService to check admin)
        if (r.getChef() != null && actorEmail != null && !actorEmail.equals(r.getChef().getEmail())) {
            // TODO: integrate with roles (if actor is admin allow)
            throw new SecurityException("not owner");
        }

        if (update.getTitle() != null) r.setTitle(update.getTitle());
        if (update.getSummary() != null) r.setSummary(update.getSummary());
        if (update.getIngredients() != null) r.setIngredients(update.getIngredients());
        if (update.getSteps() != null) r.setSteps(update.getSteps());
        if (update.getLabels() != null) r.setLabels(update.getLabels());
        if (update.getPublished() != null) r.setPublished(update.getPublished());

        // images: append new images if provided
        if (update.getImages() != null && !update.getImages().isEmpty()) {
            for (ImageUploadDto im : update.getImages()) {
                ImageEntity e = new ImageEntity();
                if (im.getDataBase64() != null && !im.getDataBase64().isBlank()) {
                    byte[] data = Base64.getDecoder().decode(im.getDataBase64());
                    String storedPath = fileStorageService.store(data, im.getFilename());
                    e.setPath(storedPath);
                } else {
                    e.setPath(im.getFilename());
                }
                e.setFilename(im.getFilename());
                r.addImage(e);
            }
        }

        Recipe saved = recipeRepository.save(r);
        return toResponse(saved);
    }

    @Override
    public void deleteRecipe(UUID id, String actorEmail) {
        Recipe r = recipeRepository.findById(id).orElseThrow(NoSuchElementException::new);
        if (r.getChef() != null && actorEmail != null && !actorEmail.equals(r.getChef().getEmail())) {
            throw new SecurityException("not owner");
        }
        recipeRepository.delete(r);
    }

    // --- mapping helper ---
    private RecipeResponse toResponse(Recipe r) {
        RecipeResponse out = new RecipeResponse();
        out.setId(r.getId());
        out.setTitle(r.getTitle());
        out.setSummary(r.getSummary());
        out.setIngredients(r.getIngredients());
        out.setSteps(r.getSteps());
        out.setLabels(r.getLabels());
        out.setPublished(r.isPublished());
        out.setPublishedAt(r.getPublishedAt());
        if (r.getChef() != null) {
            out.setChefId(r.getChef().getId() == null ? null : r.getChef().getId().toString());
            out.setChefHandle(r.getChef().getHandle());
            out.setChefEmail(r.getChef().getEmail());
        }
        if (r.getImages() != null) {
            var imgs = r.getImages().stream().map(e -> {
                var i = new ImageInfoDto();
                i.setId(e.getId() == null ? null : e.getId().toString());
                i.setFilename(e.getFilename());
                i.setUrl(e.getPath());
                i.setSizeLabel(e.getSizeLabel());
                return i;
            }).collect(Collectors.toList());
            out.setImages(imgs);
        }
        return out;
    }

    @Override
    public PagedResponse<RecipeResponse> searchRecipes(String q, String publishedFrom, String publishedTo,
            String chefId, String chefHandle, int page, int pageSize, String sortBy, String sortDir) {
       if (page < 0) page = 0;
        if (pageSize <= 0) pageSize = DEFAULT_PAGE_SIZE;
        if (pageSize > MAX_PAGE_SIZE) pageSize = MAX_PAGE_SIZE;

        Sort sort = Sort.by(Sort.Direction.fromString(Optional.ofNullable(sortDir).orElse("DESC")), Optional.ofNullable(sortBy).orElse("publishedAt"));
        Pageable pageable = PageRequest.of(page, pageSize, sort);

        UUID chefUuid = null;
        if (chefId != null && !chefId.isBlank()) {
            try { chefUuid = UUID.fromString(chefId); } catch (IllegalArgumentException ignored) {}
        }

        Specification<Recipe> spec = RecipeSpecifications.combine(
                RecipeSpecifications.keywordSearch(q),
                RecipeSpecifications.publishedFrom(publishedFrom),
                RecipeSpecifications.publishedTo(publishedTo),
                chefUuid == null ? null : RecipeSpecifications.byChefId(chefUuid),
                RecipeSpecifications.byChefHandle(chefHandle)
        );

        Page<Recipe> pageRes = recipeRepository.findAll(spec, pageable);

        // map entities -> DTOs (simple mapping)
        List<RecipeResponse> items = pageRes.stream().map(this::toDto).collect(Collectors.toList());

        Map<String,Object> meta = new HashMap<>();
        meta.put("page", pageRes.getNumber());
        meta.put("page_size", pageRes.getSize());
        meta.put("total_pages", pageRes.getTotalPages());
        meta.put("total_elements", pageRes.getTotalElements());

        return new PagedResponse<>(meta, items);
    }

    private RecipeResponse toDto(Recipe r) {
        RecipeResponse dto = new RecipeResponse();
        dto.setId(r.getId());
        dto.setTitle(r.getTitle());
        dto.setSummary(r.getSummary());
        dto.setIngredients(r.getIngredients());
        dto.setSteps(r.getSteps());
        dto.setLabels(r.getLabels());
        dto.setPublished(r.isPublished());
        dto.setPublishedAt(r.getPublishedAt());
        dto.setChefId(r.getChef().getId().toString());
        // map images minimally (filename/path)
        dto.setImages(r.getImages().stream().map(img -> {
            ImageInfoDto i = new ImageInfoDto();
            i.setId(img.getId() == null ? null : img.getId().toString());
            i.setFilename(img.getFilename());
            i.setUrl(img.getPath());
            i.setSizeLabel(img.getSizeLabel());
            return i;
        }).collect(Collectors.toList()));
        return dto;
    }


}

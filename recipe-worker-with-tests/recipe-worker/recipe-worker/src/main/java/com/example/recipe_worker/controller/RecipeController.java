package com.example.recipe_worker.controller;

import com.example.recipe_worker.dto.ImageUploadDto;
import com.example.recipe_worker.dto.RecipeCreateRequest;
import com.example.recipe_worker.dto.RecipeResponse;
import com.example.recipe_worker.dto.RecipeUpdateRequest;
import com.example.recipe_worker.service.FileStorageService;
import com.example.recipe_worker.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final ObjectMapper mapper = new ObjectMapper();
    private final RecipeService recipeService;
    private final FileStorageService fileStorageService;

    public RecipeController(RecipeService recipeService, FileStorageService fileStorageService) {
        this.recipeService = recipeService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping(consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    @PreAuthorize("hasAuthority('ROLE_CHEF')")
    public ResponseEntity<?> createMultipart(
            @RequestParam String title,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String ingredients,
            @RequestParam(required = false) String steps,
            @RequestParam(required = false) String labels,
            @RequestParam(required = false, defaultValue = "false") boolean published,
            @RequestParam(required = false) MultipartFile[] images,
            Authentication authentication) {

        try {
            // build DTO
            RecipeCreateRequest req = new RecipeCreateRequest();
            req.setTitle(title);
            req.setSummary(summary);
            req.setIngredients(ingredients == null ? List.of() :
                    mapper.readValue(ingredients, List.class));
            req.setSteps(steps == null ? List.of() :
                    mapper.readValue(steps, List.class));
            req.setLabels(labels == null ? List.of() :
                    mapper.readValue(labels, List.class));
            req.setPublished(published);

            // Convert multipart files to ImageUploadDto and save via file storage
            if (images != null && images.length > 0) {
                List<ImageUploadDto> imgDtos = new ArrayList<>();
                for (MultipartFile f : images) {
                    if (f == null || f.isEmpty()) continue;
                    // store bytes and get stored path (delegated to FileStorageService)
                    byte[] data = f.getBytes();
                    String storedPath = fileStorageService.store(data, f.getOriginalFilename());
                    ImageUploadDto im = new ImageUploadDto();
                    im.setFilename(f.getOriginalFilename());
                    im.setMimeType(f.getContentType());
                    im.setDataBase64(null); 
                    im.setFilename(storedPath);
                    imgDtos.add(im);
                }
                req.setImages(imgDtos);
            }

            String chefEmail = authentication != null ? authentication.getName() : null;

            RecipeResponse saved = recipeService.createRecipe(req, chefEmail);
            return ResponseEntity.ok(saved);

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "failed_to_publish", "detail", ex.getMessage()));
        }
    }

   
    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE })
    @PreAuthorize("hasAuthority('ROLE_CHEF')")
    public ResponseEntity<?> createJson(@Valid @RequestBody RecipeCreateRequest req, Authentication authentication) {
        try {
            String chefEmail = authentication != null ? authentication.getName() : null;
            RecipeResponse saved = recipeService.createRecipe(req, chefEmail);
            return ResponseEntity.ok(saved);
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "failed_to_publish", "detail", ex.getMessage()));
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable("id") UUID id) {
        var opt = recipeService.getById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CHEF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> update(@PathVariable("id") UUID id,
                                    @Valid @RequestBody RecipeUpdateRequest update,
                                    Authentication authentication) {
        try {
            String actorEmail = authentication != null ? authentication.getName() : null;
            var updated = recipeService.updateRecipe(id, update, actorEmail);
            return ResponseEntity.ok(updated);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        } catch (NoSuchElementException ne) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "failed_to_update", "detail", ex.getMessage()));
        }
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_CHEF') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable("id") UUID id, Authentication authentication) {
        try {
            String actorEmail = authentication != null ? authentication.getName() : null;
            recipeService.deleteRecipe(id, actorEmail);
            return ResponseEntity.noContent().build();
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        } catch (NoSuchElementException ne) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/debug/whoami")
    public ResponseEntity<?> whoami(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "principal", auth == null ? null : auth.getPrincipal(),
                "authorities", auth == null ? null : auth.getAuthorities()
        ));
    }


    @GetMapping
    public ResponseEntity<?> listRecipes(
            @RequestParam(required = false) String q,
            @RequestParam(required = false, name = "published_from") String publishedFrom,
            @RequestParam(required = false, name = "published_to") String publishedTo,
            @RequestParam(required = false, name = "chef_id") String chefId,
            @RequestParam(required = false, name = "chef_handle") String chefHandle,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int page_size,
            @RequestParam(required = false, defaultValue = "publishedAt") String sort_by,
            @RequestParam(required = false, defaultValue = "DESC") String sort_dir) {

        var resp = recipeService.searchRecipes(q, publishedFrom, publishedTo, chefId, chefHandle, page, page_size, sort_by, sort_dir);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Path storage = Paths.get("./data/storage").toAbsolutePath().normalize();
            Path imagePath = storage.resolve(filename).normalize();
            System.out.println("Storage dir: " + storage);
            System.out.println("Resolved imagePath: " + imagePath);
            if (!Files.exists(imagePath)) {
                System.out.println("File not found: " + imagePath);
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(imagePath.toUri());
            String contentType = Files.probeContentType(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

}

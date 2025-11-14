package com.example.recipe_worker.controller;

import com.example.recipe_worker.dto.PagedResponse;
import com.example.recipe_worker.dto.RecipeCreateRequest;
import com.example.recipe_worker.dto.RecipeResponse;
import com.example.recipe_worker.dto.RecipeUpdateRequest;
import com.example.recipe_worker.security.JwtTokenProvider;
import com.example.recipe_worker.service.FileStorageService;
import com.example.recipe_worker.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@WebMvcTest(controllers = RecipeController.class)
@AutoConfigureMockMvc(addFilters = false)  
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private FileStorageService fileStorageService;

    private final ObjectMapper mapper = new ObjectMapper();

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void createMultipart_withValidJwtHeader_shouldReturnSavedRecipe() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                "chef@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_CHEF"))
        );
    
        when(fileStorageService.store(any(), eq("pic.jpg")))
                .thenReturn("./data/storage/stored-pic.jpg");
    
        RecipeResponse resp = new RecipeResponse();
        resp.setId(UUID.randomUUID());
        resp.setTitle("Pasta");
    
        when(recipeService.createRecipe(any(RecipeCreateRequest.class), eq("chef@example.com")))
                .thenReturn(resp);
    
        MockMultipartFile img = new MockMultipartFile("images", "pic.jpg", "image/jpeg", "data".getBytes());
    
        mockMvc.perform(multipart("/api/recipes")
                        .file(img)
                        .param("title", "Pasta")
                        .param("summary", "Delicious")
                        .param("ingredients", mapper.writeValueAsString(List.of("a","b")))
                        .param("steps", mapper.writeValueAsString(List.of("s1")))
                        .param("labels", mapper.writeValueAsString(List.of("Italian")))
                        .param("published", "true")
                        .with(authentication(auth))   // <-- set SecurityContext for request
                        .with(csrf()))
                .andExpect(status().isOk());
    
        verify(fileStorageService, atLeastOnce()).store(any(), eq("pic.jpg"));
    }



    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(recipeService.getById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_found_returnsRecipe() throws Exception {
        UUID id = UUID.randomUUID();
        RecipeResponse resp = new RecipeResponse();
        resp.setId(id);
        resp.setTitle("Found");
        when(recipeService.getById(id)).thenReturn(Optional.of(resp));

        mockMvc.perform(get("/api/recipes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Found"));
    }

    @Test
    @WithMockUser(username = "chef@example.com", roles = {"CHEF"})
    void update_recipe_ok() throws Exception {
        UUID id = UUID.randomUUID();
        RecipeUpdateRequest upd = new RecipeUpdateRequest();
        upd.setTitle("New Title");
    
        RecipeResponse updated = new RecipeResponse();
        updated.setId(id);
        updated.setTitle("New Title");
    
        // More permissive on actor (accept any string)
        when(recipeService.updateRecipe(eq(id), any(RecipeUpdateRequest.class), anyString()))
                .thenReturn(updated);
    
        mockMvc.perform(patch("/api/recipes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(upd)))
                .andExpect(status().isOk());
        
    }
    
    // ---------------- delete ----------------
    @Test
    @WithMockUser(roles = {"CHEF"})
    void delete_recipe_ok() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(recipeService).deleteRecipe(eq(id), anyString());

        mockMvc.perform(delete("/api/recipes/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = {"CHEF"})
    void listRecipes_invokesService_and_returnsPaged() throws Exception {
        RecipeResponse r = new RecipeResponse();
        r.setId(UUID.randomUUID());
        r.setTitle("A");


        Map<String,Object> meta = Map.of(
                "page", 0,
                "page_size", 20,
                "total_pages", 1,
                "total_elements", 1
        );
        PagedResponse<RecipeResponse> pageResp = new PagedResponse<>(meta, List.of(r));

        when(recipeService.searchRecipes(
                anyString(), // q
                anyString(), // publishedFrom
                anyString(), // publishedTo
                anyString(), // chefId
                anyString(), // chefHandle
                anyInt(),    // page
                anyInt(),    // pageSize
                anyString(), // sortBy
                anyString()  // sortDir
        )).thenReturn(pageResp);

        mockMvc.perform(get("/api/recipes")
                        .param("page", "0")
                        .param("page_size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ---------------- get image ----------------
    @Test
    void getImage_file_exists_returnsBytes() throws Exception {
        Path base = Paths.get("./data/storage");
        Files.createDirectories(base);
        Path f = base.resolve("test-image.jpg");
        Files.write(f, "image-bytes".getBytes());

        mockMvc.perform(get("/api/recipes/images/{filename}", "test-image.jpg"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", Matchers.containsString("image")));

        Files.deleteIfExists(f);
    }

    
}

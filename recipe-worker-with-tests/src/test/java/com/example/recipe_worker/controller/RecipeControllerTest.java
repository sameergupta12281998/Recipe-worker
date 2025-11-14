package com.example.recipe_worker.controller;

import com.example.recipe_worker.dto.RecipeCreateRequest;
import com.example.recipe_worker.service.FileStorageService;
import com.example.recipe_worker.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecipeController.class)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private FileStorageService fileStorageService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void listRecipes_invokesService_and_returnsPaged() throws Exception {
        Map<String,Object> pageResp = Map.of(
                "meta", Map.of("page", 0, "page_size", 20, "total_pages", 1, "total_elements", 1),
                "data", List.of(Map.of("id", UUID.randomUUID().toString(), "title", "A"))
        );

        when(recipeService.searchRecipes(
                anyString(), anyString(), anyString(), anyString(), anyString(),
                anyInt(), anyInt(), anyString(), anyString()
        )).thenReturn(pageResp);

        mockMvc.perform(get("/api/recipes")
                        .param("page", "0")
                        .param("page_size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.data[0].title").value("A"));
    }

    @Test
    @WithMockUser(roles = {"CHEF"})
    void createMultipart_shouldStoreFiles_andReturnSaved() throws Exception {
        byte[] content = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("images", "pic.jpg", "image/jpeg", content);

        when(fileStorageService.store(any(), anyString())).thenReturn("./data/storage/saved-pic.jpg");

        Map<String,Object> resp = Map.of(
                "id", UUID.randomUUID().toString(),
                "title", "Pasta Carbonara",
                "summary", "A creamy pasta",
                "images", List.of(Map.of("filename","saved-pic.jpg","path","./data/storage/saved-pic.jpg"))
        );

        when(recipeService.createRecipe(any(RecipeCreateRequest.class), anyString()))
                .thenReturn(resp);

        mockMvc.perform(multipart("/api/recipes")
                        .file(file)
                        .param("title", "Pasta Carbonara")
                        .param("summary", "A creamy pasta")
                        .param("ingredients", mapper.writeValueAsString(List.of("spaghetti","eggs")))
                        .param("steps", mapper.writeValueAsString(List.of("Boil","Mix")))
                        .param("labels", mapper.writeValueAsString(List.of("Italian")))
                        .param("published", "true")
                        .with(csrf())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pasta Carbonara"));
    }
}

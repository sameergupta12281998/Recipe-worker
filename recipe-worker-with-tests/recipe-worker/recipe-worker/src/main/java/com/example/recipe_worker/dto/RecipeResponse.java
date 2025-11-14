package com.example.recipe_worker.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class RecipeResponse {

    private UUID id;
    private String title;
    private String summary;
    private List<String> ingredients;
    private List<String> steps;
    private List<String> labels;
    private boolean published;
    private Instant publishedAt;
    private String chefId;     
    private String chefHandle;
    private String chefEmail;
    private List<ImageInfoDto> images;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public List<String> getLabels() { return labels; }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Instant publishedAt) { this.publishedAt = publishedAt; }
    public String getChefId() { return chefId; }
    public void setChefId(String chefId) { this.chefId = chefId; }
    public String getChefHandle() { return chefHandle; }
    public void setChefHandle(String chefHandle) { this.chefHandle = chefHandle; }
    public String getChefEmail() { return chefEmail; }
    public void setChefEmail(String chefEmail) { this.chefEmail = chefEmail; }
    public List<ImageInfoDto> getImages() { return images; }
    public void setImages(List<ImageInfoDto> images) { this.images = images; }
}


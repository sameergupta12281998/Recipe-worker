package com.example.recipe_worker.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public class RecipeUpdateRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String summary;

    private List<String> ingredients;
    private List<String> steps;
    private List<String> labels;

    // use Boolean to allow null (no change) vs true/false (explicit change)
    private Boolean published;

    private List<ImageUploadDto> images; // images to add (or empty)

    // getters / setters
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
    public Boolean getPublished() { return published; }
    public void setPublished(Boolean published) { this.published = published; }
    public List<ImageUploadDto> getImages() { return images; }
    public void setImages(List<ImageUploadDto> images) { this.images = images; }
}

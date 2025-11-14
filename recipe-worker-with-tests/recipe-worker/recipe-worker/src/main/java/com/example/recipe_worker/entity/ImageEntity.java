package com.example.recipe_worker.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "image_entity")
public class ImageEntity {

    @Id
    private UUID id;

    @Column(length = 500)
    private String filename;

    @Column(length = 1000)
    private String path; // stored file path or URL

    @Column(name = "size_label", length = 50)
    private String sizeLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    public ImageEntity() {}

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    // getters / setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
}

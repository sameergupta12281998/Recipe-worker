package com.example.recipe_worker.dto;

import com.example.recipe_worker.entity.ImageEntity;
import com.example.recipe_worker.entity.Recipe;

import java.util.List;
import java.util.stream.Collectors;

public class RecipeMapper {

    public static RecipeResponse toResponse(Recipe r) {
        if (r == null) return null;
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
        List<ImageInfoDto> imgs = r.getImages() == null ? List.of() :
            r.getImages().stream().map(RecipeMapper::toImageInfo).collect(Collectors.toList());
        out.setImages(imgs);
        return out;
    }

    public static ImageInfoDto toImageInfo(ImageEntity e) {
        ImageInfoDto info = new ImageInfoDto();
        info.setId(e.getId() == null ? null : e.getId().toString());
        info.setFilename(e.getFilename());
        // compute URL/path if you have a mapping endpoint e.g. /images/{id}/{size}
        info.setUrl("/images/" + e.getId()); 
        info.setSizeLabel(e.getSizeLabel());
        return info;
    }
}

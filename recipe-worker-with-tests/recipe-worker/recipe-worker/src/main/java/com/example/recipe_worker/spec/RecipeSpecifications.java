package com.example.recipe_worker.spec;

import com.example.recipe_worker.entity.Recipe;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.UUID;

public final class RecipeSpecifications {

    private RecipeSpecifications() {}

    public static Specification<Recipe> publishedFrom(String isoDate) {
        if (isoDate == null) return null;
        try {
            Instant from = Instant.parse(isoDate);
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("publishedAt"), from);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static Specification<Recipe> publishedTo(String isoDate) {
        if (isoDate == null) return null;
        try {
            Instant to = Instant.parse(isoDate);
            return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("publishedAt"), to);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public static Specification<Recipe> byChefId(UUID chefId) {
        if (chefId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("chefId"), chefId);
    }

    public static Specification<Recipe> byChefHandle(String handle) {
        if (handle == null || handle.isBlank()) return null;
        // assuming Recipe has a chef handle — if not, you'll need to join Chef entity
        return (root, query, cb) -> cb.equal(cb.lower(root.get("chefHandle")), handle.toLowerCase(Locale.ROOT));
    }

    public static Specification<Recipe> keywordSearch(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            // ensure distinct results because of joins
            query.distinct(true);

            // title & summary
            Predicate pTitle = cb.like(cb.lower(root.get("title")), like);
            Predicate pSummary = cb.like(cb.lower(root.get("summary")), like);

            // join ingredients (ElementCollection)
            Predicate pIngredients = cb.literal(false).isNotNull();
            Predicate pSteps = cb.literal(false).isNotNull();
            try {
                Join<Recipe, String> ingJoin = root.join("ingredients", JoinType.LEFT);
                pIngredients = cb.like(cb.lower(ingJoin), like);
            } catch (IllegalArgumentException ignored) {
                // field may not exist — ignore
            }

            try {
                Join<Recipe, String> stepJoin = root.join("steps", JoinType.LEFT);
                pSteps = cb.like(cb.lower(stepJoin), like);
            } catch (IllegalArgumentException ignored) { }

            return cb.or(pTitle, pSummary, pIngredients, pSteps);
        };
    }

    public static Specification<Recipe> combine(Specification<Recipe>... specs) {
        Specification<Recipe> result = null;
        for (Specification<Recipe> s : specs) {
            if (s == null) continue;
            result = result == null ? s : result.and(s);
        }
        return result;
    }
}

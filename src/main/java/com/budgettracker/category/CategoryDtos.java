package com.budgettracker.category;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {
    private CategoryDtos() {
    }

    private static final String HEX = "^#[0-9A-Fa-f]{6}$";

    public record CreateCategoryRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = HEX, message = "must be a hex colour like #FF8800") String colour) {
    }

    public record EditCategoryRequest(
            @NotNull Long id,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = HEX, message = "must be a hex colour like #FF8800") String colour) {
    }

    public record CategoryResponse(Long id, String name, String colour, Instant createdAt) {
        static CategoryResponse from(Category c) {
            return new CategoryResponse(c.getId(), c.getName(), c.getColour(), c.getCreatedAt());
        }
    }
}

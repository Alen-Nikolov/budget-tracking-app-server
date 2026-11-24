package com.budgettracker.category;

import java.util.List;

import com.budgettracker.category.CategoryDtos.CategoryResponse;
import com.budgettracker.category.CategoryDtos.CreateCategoryRequest;
import com.budgettracker.category.CategoryDtos.EditCategoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The user id always comes from the JWT (@AuthenticationPrincipal), never from the request body. */
@RestController
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list(@AuthenticationPrincipal Long userId) {
        return service.list(userId);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateCategoryRequest req) {
        return service.create(userId, req);
    }

    @PutMapping("/edit")
    public CategoryResponse edit(@AuthenticationPrincipal Long userId, @Valid @RequestBody EditCategoryRequest req) {
        return service.edit(userId, req);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}

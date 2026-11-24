package com.budgettracker.category;

import java.util.List;

import com.budgettracker.category.CategoryDtos.CategoryResponse;
import com.budgettracker.category.CategoryDtos.CreateCategoryRequest;
import com.budgettracker.category.CategoryDtos.EditCategoryRequest;
import com.budgettracker.common.ConflictException;
import com.budgettracker.common.NotFoundException;
import com.budgettracker.expense.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categories;
    private final ExpenseRepository expenses;

    public CategoryService(CategoryRepository categories, ExpenseRepository expenses) {
        this.categories = categories;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categories.findByUserIdOrderByNameAsc(userId).stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CreateCategoryRequest req) {
        String name = req.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("You already have a category named '" + name + "'");
        }
        return CategoryResponse.from(categories.save(new Category(userId, name, req.colour())));
    }

    @Transactional
    public CategoryResponse edit(Long userId, EditCategoryRequest req) {
        Category category = find(userId, req.id());
        String name = req.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, category.getId())) {
            throw new ConflictException("You already have a category named '" + name + "'");
        }
        category.setName(name);
        category.setColour(req.colour());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = find(userId, categoryId);
        if (expenses.existsByCategoryId(categoryId)) {
            throw new ConflictException("Category still has expenses; delete or move them first");
        }
        categories.delete(category);
    }

    private Category find(Long userId, Long categoryId) {
        // Scoped by user: someone else's category looks exactly like a missing one.
        return categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }
}

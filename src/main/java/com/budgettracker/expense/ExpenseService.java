package com.budgettracker.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.budgettracker.category.Category;
import com.budgettracker.category.CategoryRepository;
import com.budgettracker.common.NotFoundException;
import com.budgettracker.expense.ExpenseDtos.CategoryTotal;
import com.budgettracker.expense.ExpenseDtos.CreateExpenseRequest;
import com.budgettracker.expense.ExpenseDtos.EditExpenseRequest;
import com.budgettracker.expense.ExpenseDtos.ExpenseResponse;
import com.budgettracker.expense.ExpenseDtos.ExpenseSearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final CategoryRepository categories;

    public ExpenseService(ExpenseRepository expenses, CategoryRepository categories) {
        this.expenses = expenses;
        this.categories = categories;
    }

    @Transactional
    public ExpenseResponse create(Long userId, CreateExpenseRequest req) {
        Category category = ownedCategory(userId, req.categoryId());
        LocalDate date = req.date() != null ? req.date() : LocalDate.now();
        Expense saved = expenses.save(new Expense(userId, category, req.amount(), blankToNull(req.note()), date));
        return ExpenseResponse.from(saved);
    }

    @Transactional
    public ExpenseResponse edit(Long userId, EditExpenseRequest req) {
        Expense expense = ownedExpense(userId, req.id());
        expense.setCategory(ownedCategory(userId, req.categoryId()));
        expense.setAmount(req.amount());
        expense.setNote(blankToNull(req.note()));
        expense.setExpenseDate(req.date());
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long userId, Long expenseId) {
        expenses.delete(ownedExpense(userId, expenseId));
    }

    /** Empty/absent categoryIds means "all categories". */
    @Transactional(readOnly = true)
    public ExpenseSearchResponse search(Long userId, LocalDate from, LocalDate to, List<Long> categoryIds) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }
        List<Expense> found = (categoryIds == null || categoryIds.isEmpty())
                ? expenses.findInRange(userId, from, to)
                : expenses.findInRangeForCategories(userId, from, to, categoryIds);

        Map<Long, CategoryTotal> totals = new LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Expense e : found) {
            Category c = e.getCategory();
            grandTotal = grandTotal.add(e.getAmount());
            totals.merge(c.getId(),
                    new CategoryTotal(c.getId(), c.getName(), c.getColour(), e.getAmount()),
                    (a, b) -> new CategoryTotal(a.categoryId(), a.categoryName(), a.categoryColour(),
                            a.total().add(b.total())));
        }
        return new ExpenseSearchResponse(from, to, grandTotal, List.copyOf(totals.values()),
                found.stream().map(ExpenseResponse::from).toList());
    }

    private Category ownedCategory(Long userId, Long categoryId) {
        return categories.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    private Expense ownedExpense(Long userId, Long expenseId) {
        return expenses.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new NotFoundException("Expense not found"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

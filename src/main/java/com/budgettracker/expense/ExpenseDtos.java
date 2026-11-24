package com.budgettracker.expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ExpenseDtos {
    private ExpenseDtos() {
    }

    public record CreateExpenseRequest(
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull Long categoryId,
            @Size(max = 255) String note,
            LocalDate date) { // optional, defaults to today
    }

    public record EditExpenseRequest(
            @NotNull Long id,
            @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 10, fraction = 2) BigDecimal amount,
            @NotNull Long categoryId,
            @Size(max = 255) String note,
            @NotNull LocalDate date) {
    }

    public record ExpenseResponse(Long id, BigDecimal amount, String note, LocalDate date,
                                  Long categoryId, String categoryName, String categoryColour) {
        static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getAmount(), e.getNote(), e.getExpenseDate(),
                    e.getCategory().getId(), e.getCategory().getName(), e.getCategory().getColour());
        }
    }

    public record CategoryTotal(Long categoryId, String categoryName, String categoryColour, BigDecimal total) {
    }

    /** Result of the filter endpoint: the matching expenses plus the totals the app needs for its charts. */
    public record ExpenseSearchResponse(LocalDate from, LocalDate to, BigDecimal total,
                                        List<CategoryTotal> byCategory, List<ExpenseResponse> expenses) {
    }
}

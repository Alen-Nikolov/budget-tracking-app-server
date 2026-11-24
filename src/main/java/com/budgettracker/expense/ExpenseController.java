package com.budgettracker.expense;

import java.time.LocalDate;
import java.util.List;

import com.budgettracker.expense.ExpenseDtos.CreateExpenseRequest;
import com.budgettracker.expense.ExpenseDtos.EditExpenseRequest;
import com.budgettracker.expense.ExpenseDtos.ExpenseResponse;
import com.budgettracker.expense.ExpenseDtos.ExpenseSearchResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    /** GET /expenses?from=2026-01-01&to=2026-01-31&categoryIds=1&categoryIds=2  (categoryIds optional) */
    @GetMapping
    public ExpenseSearchResponse search(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<Long> categoryIds) {
        return service.search(userId, from, to, categoryIds);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@AuthenticationPrincipal Long userId, @Valid @RequestBody CreateExpenseRequest req) {
        return service.create(userId, req);
    }

    @PutMapping("/edit")
    public ExpenseResponse edit(@AuthenticationPrincipal Long userId, @Valid @RequestBody EditExpenseRequest req) {
        return service.edit(userId, req);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}

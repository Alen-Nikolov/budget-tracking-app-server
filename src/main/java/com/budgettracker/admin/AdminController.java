package com.budgettracker.admin;

import java.math.BigDecimal;

import com.budgettracker.expense.ExpenseRepository;
import com.budgettracker.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only (enforced in SecurityConfig). The admin's own expenses/categories use the normal
 * endpoints, since the admin is also a user.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    public record StatsResponse(long totalUsers, long totalExpenses, BigDecimal totalAmount) {
    }

    private final UserRepository users;
    private final ExpenseRepository expenses;

    public AdminController(UserRepository users, ExpenseRepository expenses) {
        this.users = users;
        this.expenses = expenses;
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(users.count(), expenses.count(), expenses.sumAllAmounts());
    }
}

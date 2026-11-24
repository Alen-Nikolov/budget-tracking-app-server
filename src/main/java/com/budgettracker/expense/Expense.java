package com.budgettracker.expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.budgettracker.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    private String note;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Expense() {
    }

    public Expense(Long userId, Category category, BigDecimal amount, String note, LocalDate expenseDate) {
        this.userId = userId;
        this.category = category;
        this.amount = amount;
        this.note = note;
        this.expenseDate = expenseDate;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Category getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getNote() { return note; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public Instant getCreatedAt() { return createdAt; }

    public void setCategory(Category category) { this.category = category; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public void setNote(String note) { this.note = note; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
}

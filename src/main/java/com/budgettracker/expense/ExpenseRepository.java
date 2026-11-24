package com.budgettracker.expense;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e")
    java.math.BigDecimal sumAllAmounts();

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            select e from Expense e join fetch e.category
            where e.userId = :userId and e.expenseDate between :from and :to
            order by e.expenseDate desc, e.id desc""")
    List<Expense> findInRange(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select e from Expense e join fetch e.category c
            where e.userId = :userId and e.expenseDate between :from and :to and c.id in :categoryIds
            order by e.expenseDate desc, e.id desc""")
    List<Expense> findInRangeForCategories(@Param("userId") Long userId, @Param("from") LocalDate from,
                                           @Param("to") LocalDate to, @Param("categoryIds") Collection<Long> categoryIds);
}

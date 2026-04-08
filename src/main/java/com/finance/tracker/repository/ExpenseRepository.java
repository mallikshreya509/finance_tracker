package com.finance.tracker.repository;

import com.finance.tracker.model.Expense;
import com.finance.tracker.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByUserOrderByExpenseDateDesc(User user, Pageable pageable);

    List<Expense> findByUserOrderByExpenseDateDesc(User user);

    List<Expense> findByUserAndExpenseDateBetweenOrderByExpenseDateDesc(
        User user, LocalDate start, LocalDate end);

    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
           "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findByUserAndYearAndMonth(@Param("user") User user,
                                             @Param("year") int year,
                                             @Param("month") int month);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.user = :user " +
           "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    BigDecimal sumAmountByUserAndMonth(@Param("user") User user,
                                       @Param("year") int year,
                                       @Param("month") int month);

    @Query("SELECT e.category.name, SUM(e.amount) FROM Expense e WHERE e.user = :user " +
           "AND YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month " +
           "GROUP BY e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumByCategoryForMonth(@Param("user") User user,
                                          @Param("year") int year,
                                          @Param("month") int month);

    @Query("SELECT e FROM Expense e WHERE e.user = :user " +
           "AND LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Expense> searchByDescription(@Param("user") User user, @Param("keyword") String keyword);

    @Query("SELECT AVG(e.amount) FROM Expense e WHERE e.user = :user " +
           "AND e.category.id = :categoryId")
    BigDecimal avgAmountByCategory(@Param("user") User user, @Param("categoryId") Long categoryId);
}

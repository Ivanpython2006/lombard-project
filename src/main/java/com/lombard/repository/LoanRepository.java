package com.lombard.repository;

import com.lombard.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByClientId(Long clientId);

    @Query("SELECT AVG(l.amount) FROM Loan l")
    Double getAverageLoanAmount();

    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> countByStatus();

    @Query("SELECT l.status, SUM(l.amount) FROM Loan l GROUP BY l.status")
    List<Object[]> sumByStatus();

    // даты подачи заявки и решения по ней; среднее считаю в сервисе,
    // потому что разница дат в SQL по-разному работает в PostgreSQL и H2
    @Query("SELECT l.createdDate, l.decisionDate FROM Loan l " +
           "WHERE l.decisionDate IS NOT NULL AND l.createdDate IS NOT NULL")
    List<Object[]> findDecisionDates();
}
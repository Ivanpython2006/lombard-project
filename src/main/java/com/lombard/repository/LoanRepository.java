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

    @Query(value = "SELECT AVG(decision_date - created_date) FROM loans " +
                   "WHERE decision_date IS NOT NULL AND created_date IS NOT NULL",
           nativeQuery = true)
    Double getAverageWaitingDays();
}
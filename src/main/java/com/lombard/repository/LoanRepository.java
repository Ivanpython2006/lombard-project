package com.lombard.repository;

import com.lombard.model.Loan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByClientId(Long clientId);

    // блокирую строку займа на время покупки, чтобы два клиента не купили одну вещь одновременно
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Loan l WHERE l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT AVG(COALESCE(l.agreedAmount, l.amount)) FROM Loan l")
    Double getAverageLoanAmount();

    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> countByStatus();

    @Query("SELECT l.status, SUM(COALESCE(l.agreedAmount, l.amount)) FROM Loan l GROUP BY l.status")
    List<Object[]> sumByStatus();

    // даты подачи заявки и решения по ней; среднее считаю в сервисе,
    // потому что разница дат в SQL по-разному работает в PostgreSQL и H2
    @Query("SELECT l.createdDate, l.decisionDate FROM Loan l " +
           "WHERE l.decisionDate IS NOT NULL AND l.createdDate IS NOT NULL")
    List<Object[]> findDecisionDates();
}
package com.lombard.repository;

import com.lombard.model.LoanHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanHistoryRepository extends JpaRepository<LoanHistory, Long> {

    List<LoanHistory> findByLoanIdOrderByChangedAtDesc(Long loanId);

    List<LoanHistory> findByChangedByOrderByChangedAtDesc(String changedBy);

    List<LoanHistory> findByChangedByAndChangedAtBetweenOrderByChangedAtDesc(
            String changedBy, LocalDateTime from, LocalDateTime to);

    void deleteByLoanId(Long loanId);
}
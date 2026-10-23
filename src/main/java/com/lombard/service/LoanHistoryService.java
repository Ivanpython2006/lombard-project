package com.lombard.service;

import com.lombard.model.Loan;
import com.lombard.model.LoanHistory;
import com.lombard.repository.LoanHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LoanHistoryService {

    private final LoanHistoryRepository loanHistoryRepository;

    public LoanHistoryService(LoanHistoryRepository loanHistoryRepository) {
        this.loanHistoryRepository = loanHistoryRepository;
    }

    @Transactional
    public void log(Loan loan, String action, String changedBy) {
        LoanHistory history = new LoanHistory(loan, action, changedBy);
        loanHistoryRepository.save(history);
    }

    public List<LoanHistory> findByLoanId(Long loanId) {
        return loanHistoryRepository.findByLoanIdOrderByChangedAtDesc(loanId);
    }
}
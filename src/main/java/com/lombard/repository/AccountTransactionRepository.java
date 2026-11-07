package com.lombard.repository;

import com.lombard.model.AccountTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    List<AccountTransaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
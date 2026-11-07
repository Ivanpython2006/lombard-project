package com.lombard.repository;

import com.lombard.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByClientIdOrderByAddedAtDesc(Long clientId);
    Optional<CartItem> findByClientIdAndLoanId(Long clientId, Long loanId);
    long countByClientId(Long clientId);
    void deleteByClientId(Long clientId);
    void deleteByLoanId(Long loanId);
}
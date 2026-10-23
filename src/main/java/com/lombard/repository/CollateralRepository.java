package com.lombard.repository;

import com.lombard.model.Collateral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CollateralRepository extends JpaRepository<Collateral, Long> {

    List<Collateral> findByLoanId(Long loanId);

    @Query("SELECT c FROM Collateral c WHERE " +
           "LOWER(c.itemNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.name)       LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.loan.client.fullName) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Collateral> searchByAnyField(@Param("q") String query);

    void deleteByLoanId(Long loanId);
}
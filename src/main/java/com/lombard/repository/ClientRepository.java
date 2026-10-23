package com.lombard.repository;

import com.lombard.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByFullNameContainingIgnoreCase(String fullName);

    Optional<Client> findByUserId(Long userId);

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.clientNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.fullName)     LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.email)        LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "c.phone               LIKE CONCAT('%', :q, '%')")
    List<Client> searchByAnyField(@Param("q") String query);
}
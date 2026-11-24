package com.lombard.repository;

import com.lombard.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientIdOrderByCreatedAtDesc(Long clientId);

    @Modifying
    @Query("update Order o set o.status = :newStatus where o.id = :id and o.status = :oldStatus")
    int updateStatusIfUnchanged(@Param("id") Long id, @Param("oldStatus") String oldStatus,
                                @Param("newStatus") String newStatus);
}

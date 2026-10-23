package com.lombard.repository;

import com.lombard.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadTrueOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);
}
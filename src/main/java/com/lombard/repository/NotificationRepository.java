package com.lombard.repository;

import com.lombard.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadTrueOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    // удаляет все уведомления пользователя (перед удалением его учетной записи)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
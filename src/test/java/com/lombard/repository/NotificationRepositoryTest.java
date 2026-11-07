package com.lombard.repository;

import com.lombard.model.Notification;
import com.lombard.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

// Проверка выборки непрочитанных уведомлений
@DataJpaTest
class NotificationRepositoryTest {

    @Autowired NotificationRepository notificationRepository;
    @Autowired TestEntityManager em;

    @Test
    void findUnread_oneUnread_returnsIt() {
        User u = new User(); u.setUsername("u"); u.setPassword("p"); u.setRole("CLIENT");
        em.persist(u);
        Notification n = new Notification(u, "EVENT", "INFO", "arg");
        n.setRead(false);
        notificationRepository.save(n);
        em.flush();

        assertThat(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(u.getId())).hasSize(1);
    }
}
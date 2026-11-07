package com.lombard.service;

import com.lombard.model.Client;
import com.lombard.model.Notification;
import com.lombard.model.User;
import com.lombard.repository.NotificationRepository;
import com.lombard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void notify(Client client, String eventType, String type, String... args) {
        if (client == null || client.getUser() == null) return;
        notificationRepository.save(new Notification(client.getUser(), eventType, type, args));
    }

    @Transactional
    public void notifyStaff(String eventType, String type, String... args) {
        List<User> staff = new ArrayList<>();
        staff.addAll(userRepository.findByRole("MANAGER"));
        staff.addAll(userRepository.findByRole("ADMIN"));
        for (User u : staff) {
            notificationRepository.save(new Notification(u, eventType, type, args));
        }
    }

    @Transactional
    public void notifyUser(User user, String eventType, String type, String... args) {
        if (user == null) return;
        notificationRepository.save(new Notification(user, eventType, type, args));
    }

    public List<Notification> findByUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> findUnreadByUser(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public List<Notification> findReadByUser(Long userId) {
        return notificationRepository.findByUserIdAndReadTrueOrderByCreatedAtDesc(userId);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public boolean markOneRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId).orElse(null);
        if (n == null || n.getUser() == null || !n.getUser().getId().equals(userId)) {
            return false;
        }
        n.setRead(true);
        notificationRepository.save(n);
        return true;
    }

    @Transactional
    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }
}
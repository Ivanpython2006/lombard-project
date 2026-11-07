package com.lombard.service;

import com.lombard.model.Client;
import com.lombard.model.Notification;
import com.lombard.model.User;
import com.lombard.repository.NotificationRepository;
import com.lombard.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Тесты сервиса уведомлений: кому отправляются уведомления и как они помечаются прочитанными.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);
    }

    private User user(long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    @Test
    void notify_clientWithUser_savesNotification() {
        Client client = new Client();
        client.setUser(user(1L));

        notificationService.notify(client, "EVENT", "INFO", "arg1");

        verify(notificationRepository).save(any(Notification.class));
    }

    // клиент без аккаунта или null — уведомлять некого, ничего не сохраняем
    @Test
    void notify_clientWithoutUser_doesNothing() {
        notificationService.notify(null, "EVENT", "INFO");
        notificationService.notify(new Client(), "EVENT", "INFO");

        verifyNoInteractions(notificationRepository);
    }

    // сотрудники = все менеджеры + все админы, каждому своё уведомление
    @Test
    void notifyStaff_managersAndAdmins_savesForEach() {
        when(userRepository.findByRole("MANAGER")).thenReturn(List.of(user(2L)));
        when(userRepository.findByRole("ADMIN")).thenReturn(List.of(user(3L)));

        notificationService.notifyStaff("EVENT", "WARNING", "arg1");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void notifyUser_nullUser_doesNothing() {
        notificationService.notifyUser(null, "EVENT", "INFO");

        verifyNoInteractions(notificationRepository);
    }

    @Test
    void notifyUser_user_savesNotification() {
        notificationService.notifyUser(user(1L), "EVENT", "INFO");

        verify(notificationRepository).save(any(Notification.class));
    }

    // все три списка (все, непрочитанные, прочитанные) берутся из репозитория
    @Test
    void findByUser_allReadAndUnread_returnLists() {
        Notification n = new Notification();
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(notificationRepository.findByUserIdAndReadTrueOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));

        assertEquals(1, notificationService.findByUser(1L).size());
        assertTrue(notificationService.findUnreadByUser(1L).isEmpty());
        assertEquals(1, notificationService.findReadByUser(1L).size());
    }

    @Test
    void countUnread_returnsRepositoryCount() {
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(5L);

        assertEquals(5L, notificationService.countUnread(1L));
    }

    @Test
    void markAllRead_unreadNotifications_becomeRead() {
        Notification n = new Notification();
        n.setRead(false);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(n));

        notificationService.markAllRead(1L);

        assertTrue(n.getRead());
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    void markOneRead_notFound_returnsFalse() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(notificationService.markOneRead(99L, 1L));
    }

    // чужое уведомление прочитать нельзя
    @Test
    void markOneRead_otherUser_returnsFalse() {
        Notification n = new Notification();
        n.setUser(user(1L));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertFalse(notificationService.markOneRead(1L, 999L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markOneRead_ownNotification_returnsTrue() {
        Notification n = new Notification();
        n.setUser(user(1L));
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        assertTrue(notificationService.markOneRead(1L, 1L));
        assertTrue(n.getRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void delete_callsRepository() {
        notificationService.delete(1L);

        verify(notificationRepository).deleteById(1L);
    }
}

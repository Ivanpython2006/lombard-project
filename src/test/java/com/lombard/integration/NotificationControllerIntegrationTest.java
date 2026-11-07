package com.lombard.integration;

import com.lombard.model.Notification;
import com.lombard.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты уведомлений: после действия проверяем флаг прочтения в базе.

class NotificationControllerIntegrationTest extends IntegrationTestBase {

    private User clientUser;

    private User adminUser;

    private Notification clientNotification;

    private Notification adminNotification;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        clientUser = new User("client1", "pass", "CLIENT");
        clientUser.setUserNumber("U_C1");
        userRepository.save(clientUser);

        adminUser = new User("admin", "pass", "ADMIN");
        adminUser.setUserNumber("A1");
        userRepository.save(adminUser);

        clientNotification = new Notification(clientUser, "TEST", "INFO", "arg1");
        notificationRepository.save(clientNotification);

        adminNotification = new Notification(adminUser, "TEST", "INFO", "arg1");
        notificationRepository.save(adminNotification);
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void clientNotifications_open() throws Exception {
        mvc.perform(get("/client/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications"))
                .andExpect(model().attribute("unreadCount", 1L));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void clientNotifications_readTab_opens() throws Exception {
        mvc.perform(get("/client/notifications").param("tab", "read"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tab", "read"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void markOne_ownNotification_becomesRead() throws Exception {
        mvc.perform(post("/client/notifications/" + clientNotification.getId() + "/read").with(csrf()))
                .andExpect(status().is3xxRedirection());
        Notification updated = notificationRepository.findById(clientNotification.getId()).orElseThrow();
        assertTrue(updated.getRead());
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void markAllRead_client_allRead() throws Exception {
        mvc.perform(post("/client/notifications/mark-read").with(csrf()))
                .andExpect(status().is3xxRedirection());
        Notification updated = notificationRepository.findById(clientNotification.getId()).orElseThrow();
        assertTrue(updated.getRead());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void staffNotifications_open() throws Exception {
        mvc.perform(get("/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(model().attributeExists("notifications"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void markAllRead_admin_allRead() throws Exception {
        mvc.perform(post("/admin/notifications/mark-read").with(csrf()))
                .andExpect(status().is3xxRedirection());
        Notification updated = notificationRepository.findById(adminNotification.getId()).orElseThrow();
        assertTrue(updated.getRead());
    }
}

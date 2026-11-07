package com.lombard.controller;

import com.lombard.model.Notification;
import com.lombard.model.User;
import com.lombard.service.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты страниц уведомлений клиента и сотрудников
@WebMvcTest(controllers = NotificationController.class)
class NotificationControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void clientNotifications_unreadTab_opens() throws Exception {
        User u = new User(); u.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(notificationService.countUnread(1L)).thenReturn(3L);
        when(notificationService.findUnreadByUser(1L)).thenReturn(List.of(new Notification()));
        when(notificationService.findByUser(1L)).thenReturn(List.of(new Notification()));

        mvc.perform(get("/client/notifications").param("tab", "unread"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void clientNotifications_readTab_opens() throws Exception {
        User u = new User(); u.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(notificationService.countUnread(1L)).thenReturn(0L);
        when(notificationService.findReadByUser(1L)).thenReturn(List.of(new Notification()));
        when(notificationService.findByUser(1L)).thenReturn(List.of(new Notification()));

        mvc.perform(get("/client/notifications").param("tab", "read"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void markAllRead_callsService() throws Exception {
        User u = new User(); u.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        mvc.perform(post("/client/notifications/mark-read").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(notificationService).markAllRead(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER", username = "mgr")
    void staffNotifications_asManager_opens() throws Exception {
        User u = new User(); u.setId(2L);
        when(userService.findByUsername("mgr")).thenReturn(u);
        when(notificationService.countUnread(2L)).thenReturn(1L);
        when(notificationService.findUnreadByUser(2L)).thenReturn(List.of());
        when(notificationService.findByUser(2L)).thenReturn(List.of());

        mvc.perform(get("/admin/notifications")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void clientNotifications_opens() throws Exception {
        User u = new User(); u.setId(1L); u.setUsername("user");
        when(userService.findByUsername("user")).thenReturn(u);
        mvc.perform(get("/client/notifications")).andExpect(status().isOk());
    }
}

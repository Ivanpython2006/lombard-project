package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты смены пароля в профиле
@WebMvcTest(controllers = ProfileController.class)
class ProfileControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(username = "user")
    void passwordForm_opens() throws Exception {
        mvc.perform(get("/profile/password")).andExpect(status().isOk());
    }

    // пароль короче 6 символов
    @Test
    @WithMockUser(username = "user")
    void changePassword_tooShort_notChanged() throws Exception {
        mvc.perform(post("/profile/password").with(csrf())
                        .param("currentPassword", "old").param("newPassword", "123").param("confirmPassword", "123"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user")
    void changePassword_confirmMismatch_notChanged() throws Exception {
        mvc.perform(post("/profile/password").with(csrf())
                        .param("currentPassword", "old").param("newPassword", "123456").param("confirmPassword", "654321"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user")
    void changePassword_wrongCurrent_notChanged() throws Exception {
        when(userService.checkPassword("user", "wrong")).thenReturn(false);
        mvc.perform(post("/profile/password").with(csrf())
                        .param("currentPassword", "wrong").param("newPassword", "123456").param("confirmPassword", "123456"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "user")
    void changePassword_validData_changes() throws Exception {
        when(userService.checkPassword("user", "old")).thenReturn(true);
        mvc.perform(post("/profile/password").with(csrf())
                        .param("currentPassword", "old").param("newPassword", "123456").param("confirmPassword", "123456"))
                .andExpect(status().is3xxRedirection());
        verify(userService).changePassword("user", "123456");
    }

}

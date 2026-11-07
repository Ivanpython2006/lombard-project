package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.model.User;
import com.lombard.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты входа и регистрации нового клиента
@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser
    void loginPage_opens() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void registerPage_opens() throws Exception {
        mvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @WithMockUser
    void register_badPhone_returnsFormWithError() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "user")
                        .param("password", "123456")
                        .param("firstName", "Иван")
                        .param("lastName", "Иванов")
                        .param("email", "a@b.ru")
                        .param("phone", "123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void register_validData_createsUserAndClient() throws Exception {
        when(userService.findByUsername("newuser")).thenReturn(null);
        when(userService.register(anyString(), anyString(), anyString())).thenReturn(new User());
        when(clientService.findByUserId(any())).thenReturn(null);

        mvc.perform(post("/register").with(csrf())
                        .param("username", "newuser")
                        .param("password", "123456")
                        .param("firstName", "Иван")
                        .param("lastName", "Иванов")
                        .param("email", "a@b.ru")
                        .param("phone", "+79991112233"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clientService).save(any(Client.class));
    }

    @Test
    @WithMockUser
    void register_passwordsDoNotMatch_returnsForm() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "newuser")
                        .param("password", "123456")
                        .param("confirmPassword", "654321")
                        .param("firstName", "Иван")
                        .param("lastName", "Иванов")
                        .param("email", "a@b.ru")
                        .param("phone", "+79991112233"))
                .andExpect(status().is3xxRedirection());
    }
}

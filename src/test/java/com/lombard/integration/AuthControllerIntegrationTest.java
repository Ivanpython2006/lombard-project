package com.lombard.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты входа и регистрации: новые пользователи реально сохраняются в H2

class AuthControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    void loginPage_opens() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPage_opens() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void register_validData_createsUserAndClient() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "newuser")
                        .param("password", "password123")
                        .param("lastName", "Иванов")
                        .param("firstName", "Иван")
                        // отчество не заполнено - браузер присылает пустую строку
                        .param("middleName", "")
                        .param("phone", "+79991112233")
                        .param("email", "ivan@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertTrue(userRepository.findByUsername("newuser").isPresent());
    }

    // второй раз с тем же телефоном зарегистрироваться нельзя
    @Test
    void register_samePhoneTwice_secondFails() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username", "u1").param("password", "password123")
                .param("lastName", "Петров").param("firstName", "Петр")
                .param("phone", "+79998887766").param("email", "p1@test.com"));

        mvc.perform(post("/register").with(csrf())
                        .param("username", "u2").param("password", "password123")
                        .param("lastName", "Сидоров").param("firstName", "Сидор")
                        .param("phone", "+79998887766").param("email", "p2@test.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void register_sameEmailTwice_secondFails() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("username", "u3").param("password", "password123")
                .param("lastName", "А").param("firstName", "Б")
                .param("phone", "+79990000003").param("email", "same@test.com"));

        mvc.perform(post("/register").with(csrf())
                        .param("username", "u4").param("password", "password123")
                        .param("lastName", "В").param("firstName", "Г")
                        .param("phone", "+79990000004").param("email", "same@test.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void register_badPhone_fails() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "u5").param("password", "password123")
                        .param("lastName", "Тестов").param("firstName", "Тест")
                        .param("phone", "8999").param("email", "t@test.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void register_shortPassword_fails() throws Exception {
        mvc.perform(post("/register").with(csrf())
                        .param("username", "u6").param("password", "123")
                        .param("lastName", "Тестов").param("firstName", "Тест")
                        .param("phone", "+79990000006").param("email", "t6@test.com"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void accessDeniedPage_opens() throws Exception {
        mvc.perform(get("/403")).andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }

    // логин client1 уже занят: показываем форму с ошибкой, клиент не создаётся
    @Test
    void register_takenUsername_returnsFormWithError() throws Exception {
        long clientsBefore = clientRepository.count();

        mvc.perform(post("/register").with(csrf())
                        .param("username", "client1")
                        .param("password", "password123")
                        .param("lastName", "Петров")
                        .param("firstName", "Пётр")
                        .param("phone", "+79995554433"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        assertEquals(clientsBefore, clientRepository.count());
    }
}

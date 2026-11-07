package com.lombard.controller;

import com.lombard.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// тесты создания и редактирования клиентов админом
// после любой отправки формы контроллер делает редирект
// проверка в основном вызвался ли save() или нет
@WebMvcTest(controllers = AdminClientController.class)
@WithMockUser(roles = "ADMIN")
class AdminClientControllerTest extends ControllerTestBase {

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(1L);
    }

    // форма отправляет ФИО по частям, как настоящая страница
    private MockHttpServletRequestBuilder withName(MockHttpServletRequestBuilder req, String fullName) {
        String[] parts = fullName.isBlank() ? new String[0] : fullName.trim().split("\\s+");
        String[] names = {"lastName", "firstName", "middleName"};
        for (int i = 0; i < parts.length && i < names.length; i++) {
            req.param(names[i], parts[i]);
        }
        return req;
    }

    private MockHttpServletRequestBuilder createForm(String fullName, String phone) {
        return withName(post("/admin/clients/new").with(csrf()), fullName).param("phone", phone);
    }

    private MockHttpServletRequestBuilder editForm(String fullName, String phone) {
        when(clientService.findById(1L)).thenReturn(client);
        return withName(post("/admin/clients/1/edit").with(csrf()), fullName).param("phone", phone);
    }

    //создание

    @Test
    void newClientForm_opens() throws Exception {
        mvc.perform(get("/admin/clients/new")).andExpect(status().isOk());
    }

    @Test
    void create_validData_savesClient() throws Exception {
        mvc.perform(createForm("Иванов Иван", "+79991112233").param("email", "a@b.ru"))
                .andExpect(status().is3xxRedirection());

        // ФИО собирается из фамилии и имени
        verify(clientService).save(argThat(c -> "Иванов Иван".equals(c.getFullName())));
    }

    // фамилия и имя обязательны
    @Test
    void create_emptyOrOneWordName_notSaved() throws Exception {
        mvc.perform(createForm("", "+79991112233")).andExpect(status().is3xxRedirection());
        mvc.perform(createForm("Иван", "+79991112233")).andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    @Test
    void create_badPhone_notSaved() throws Exception {
        mvc.perform(createForm("Иванов Иван", "123")).andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    @Test
    void create_phoneTaken_notSaved() throws Exception {
        when(clientService.existsByPhone("+79991112233")).thenReturn(true);

        mvc.perform(createForm("Иванов Иван", "+79991112233")).andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    @Test
    void create_emailTaken_notSaved() throws Exception {
        when(clientService.existsByEmail("test@test.ru")).thenReturn(true);

        mvc.perform(createForm("Иванов Иван", "+79991112233").param("email", "test@test.ru"))
                .andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    //редактирование

    @Test
    void editForm_clientNotFound_redirects() throws Exception {
        when(clientService.findById(1L)).thenReturn(null);

        mvc.perform(get("/admin/clients/1/edit")).andExpect(status().is3xxRedirection());
    }

    @Test
    void edit_validData_savesClient() throws Exception {
        mvc.perform(editForm("Петров Петр", "+79992223344").param("email", "p@p.ru"))
                .andExpect(status().is3xxRedirection());

        verify(clientService).save(client);
    }

    @Test
    void edit_emptyOrOneWordName_notSaved() throws Exception {
        mvc.perform(editForm("", "+79991112233")).andExpect(status().is3xxRedirection());
        mvc.perform(editForm("Иван", "+79991112233")).andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    @Test
    void edit_badPhone_notSaved() throws Exception {
        mvc.perform(editForm("Иванов Иван", "123")).andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    // телефон занят другим клиентом (свой собственный не считается)
    @Test
    void edit_phoneTakenByOther_notSaved() throws Exception {
        when(clientService.existsByPhoneExceptId("+79991112233", 1L)).thenReturn(true);

        mvc.perform(editForm("Иванов Иван", "+79991112233")).andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }

    @Test
    void edit_emailTakenByOther_notSaved() throws Exception {
        when(clientService.existsByEmailExceptId("test@test.ru", 1L)).thenReturn(true);

        mvc.perform(editForm("Иванов Иван", "+79991112233").param("email", "test@test.ru"))
                .andExpect(status().is3xxRedirection());

        verify(clientService, never()).save(any());
    }
}

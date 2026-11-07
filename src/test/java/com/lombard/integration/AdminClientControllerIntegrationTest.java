package com.lombard.integration;

import com.lombard.model.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты создания и редактирования клиентов сотрудниками
class AdminClientControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void newClientForm_opens() throws Exception {
        mvc.perform(get("/admin/clients/new")).andExpect(status().isOk())
                .andExpect(view().name("admin_client_new"))
                .andExpect(model().attributeExists("client"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void create_validData_clientInDatabase() throws Exception {
        mvc.perform(post("/admin/clients/new").with(csrf())
                        .param("lastName", "Новый")
                        .param("firstName", "Клиент")
                        .param("phone", "+79995556677")
                        .param("email", "new@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/client/search"))
                .andExpect(flash().attributeExists("success"));
        assertTrue(clientRepository.existsByPhone("+79995556677"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_existingPhone_redirects() throws Exception {
        mvc.perform(post("/admin/clients/new").with(csrf())
                        .param("lastName", "Другой")
                        .param("firstName", "Клиент")
                        .param("phone", client.getPhone()) // телефон клиента из базы
                        .param("email", "uniq@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_badPhone_redirects() throws Exception {
        mvc.perform(post("/admin/clients/new").with(csrf())
                        .param("lastName", "Плохой")
                        .param("firstName", "Телефон")
                        .param("phone", "8999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void create_oneWordName_redirects() throws Exception {
        mvc.perform(post("/admin/clients/new").with(csrf())
                        .param("lastName", "Иван")
                        .param("phone", "+79990001122"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void editForm_opens() throws Exception {
        mvc.perform(get("/admin/clients/" + client.getId() + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_client_edit"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void edit_newName_savedToDatabase() throws Exception {
        mvc.perform(post("/admin/clients/" + client.getId() + "/edit").with(csrf())
                        .param("lastName", "Обновленный")
                        .param("firstName", "Клиент")
                        .param("phone", "+79990000000")
                        .param("email", "upd@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        assertEquals("Обновленный Клиент",
                clientRepository.findById(client.getId()).orElseThrow().getFullName());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void editForm_notFound_redirects() throws Exception {
        mvc.perform(get("/admin/clients/9999/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void newClientForm_anonymous_redirectsToLogin() throws Exception {
        mvc.perform(get("/admin/clients/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // клиент зарегистрировался сам (фамилия и имя лежат отдельно), админ меняет ФИО -
    // после сохранения должно остаться новое ФИО, а не собраться обратно старое
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void edit_clientRegisteredWithNameParts_keepsNewFullName() throws Exception {
        Client c = new Client();
        c.setLastName("Старый");
        c.setFirstName("Клиент");
        c.setPhone("+79991112244");
        c.setClientNumber("C200");
        clientRepository.save(c);

        mvc.perform(post("/admin/clients/" + c.getId() + "/edit").with(csrf())
                        .param("lastName", "Новый")
                        .param("firstName", "Клиент")
                        .param("middleName", "Петрович")
                        .param("phone", "+79991112244"))
                .andExpect(status().is3xxRedirection());
        clientRepository.flush();

        Client saved = clientRepository.findById(c.getId()).orElseThrow();
        assertEquals("Новый Клиент Петрович", saved.getFullName());
        assertEquals("Новый", saved.getLastName());
        assertEquals("Петрович", saved.getMiddleName());
    }
}

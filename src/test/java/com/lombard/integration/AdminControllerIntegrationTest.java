package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.model.User;
import com.lombard.repository.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты админки: пользователи роли импорт и статистика
class AdminControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_redirects() throws Exception {
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "newuser")
                        .param("password", "password123")
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void importUsers_csvFile_redirects() throws Exception {
        String csv = "username;password;role\nuser1;password123;CLIENT\n";
        MockMultipartFile f = new MockMultipartFile("file", "users.csv",
                "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/admin/users/import").file(f).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeRoleAndDelete_client_redirect() throws Exception {
        User target = userRepository.findByUsername("client1").orElseThrow();
        mvc.perform(post("/admin/users/" + target.getId() + "/role").with(csrf())
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void usersPages_open() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isOk());
        mvc.perform(get("/admin/users/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createUser_newManager_redirects() throws Exception {
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "newuser")
                        .param("password", "123456")
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeRole_redirects() throws Exception {
        User u = new User("bob", "p", "CLIENT"); u.setUserNumber("U1");
        userRepository.save(u);

        mvc.perform(post("/admin/users/" + u.getId() + "/role").with(csrf())
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void statsPage_opens() throws Exception {
        mvc.perform(get("/admin/stats")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "mega_user", roles = {"ADMIN"})
    void createUserAndChangeRole_redirect() throws Exception {
        mvc.perform(post("/admin/users/new").with(csrf())
                .param("username", "new_mega_user")
                .param("password", "123456")
                .param("role", "MANAGER")).andExpect(status().is3xxRedirection());

        mvc.perform(post("/admin/users/" + clientUser.getId() + "/role").with(csrf())
                .param("role", "MANAGER")).andExpect(status().is3xxRedirection());
    }

    // среднее время ожидания считается по настоящей базе H2 (раньше тут было падение ClassCastException)
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void statsPage_loanWithDecision_showsAverageWaitingDays() throws Exception {
        loan.setCreatedDate(java.time.LocalDate.now().minusDays(4));
        loan.setDecisionDate(java.time.LocalDate.now());
        loanRepository.save(loan);

        mvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_stats"))
                .andExpect(model().attribute("avgWaitingDays", 4.0));
    }
}

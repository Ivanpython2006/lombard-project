package com.lombard.integration;

import com.lombard.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты страниц менеджеров
class AdminManagerControllerIntegrationTest extends IntegrationTestBase {

    private User manager;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        manager = new User("manager1", "pass", "MANAGER");
        manager.setUserNumber("M_USER1");
        userRepository.save(manager);

        User admin = new User("admin", "pass", "ADMIN");
        admin.setUserNumber("A1");
        userRepository.save(admin);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void managersList_opens() throws Exception {
        mvc.perform(get("/admin/managers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_managers"))
                .andExpect(model().attributeExists("managers"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void managerProfile_opens() throws Exception {
        mvc.perform(get("/admin/managers/" + manager.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_manager_profile"))
                .andExpect(model().attributeExists("manager"))
                .andExpect(model().attributeExists("statsMonth"))
                .andExpect(model().attributeExists("statsAllTime"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void managerProfile_notFound_redirects() throws Exception {
        mvc.perform(get("/admin/managers/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}

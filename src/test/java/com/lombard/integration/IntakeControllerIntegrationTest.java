package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты страницы приемки заявок
class IntakeControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void intakePage_opens() throws Exception {
        mvc.perform(get("/admin/intake"))
                .andExpect(status().isOk())
                .andExpect(view().name("intake"))
                .andExpect(model().attributeExists("loans"));
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void intakePage_withFilters_opens() throws Exception {
        mvc.perform(get("/admin/intake")
                        .param("query", "L100")
                        .param("sort", "old")
                        .param("page", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void export_returnsCsv() throws Exception {
        mvc.perform(get("/admin/intake/export"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"));
    }
}

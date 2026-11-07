package com.lombard.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// Интеграционный тест главной страницы клиента
class HomeControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void homePage_asClient_opens() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}

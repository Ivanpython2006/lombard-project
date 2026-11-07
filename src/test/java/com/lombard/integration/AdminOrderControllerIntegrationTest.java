package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты заказов в админке на стандартном наборе данных
class AdminOrderControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatus_paidOrder_redirects() throws Exception {
        mvc.perform(post("/admin/orders/" + order.getId() + "/status").with(csrf())
                        .param("status", "Доставлен"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void ordersList_opens() throws Exception {
        mvc.perform(get("/admin/orders")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void orderDetails_opens() throws Exception {
        mvc.perform(get("/admin/orders/" + order.getId())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatusSeveralTimes_redirects() throws Exception {
        mvc.perform(post("/admin/orders/" + order.getId() + "/status").with(csrf())
                .param("status", "Доставлен")).andExpect(status().is3xxRedirection());
    }
}

package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты заказов клиента.

class OrderControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void ordersList_opens() throws Exception {
        mvc.perform(get("/client/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("client_orders"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void ordersList_withFilter_opens() throws Exception {
        mvc.perform(get("/client/orders")
                        .param("delivery", "PICKUP")
                        .param("sort", "date_asc")
                        .param("query", "O100"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void orderDetails_opens() throws Exception {
        mvc.perform(get("/client/orders/" + order.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("client_order_details"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void orderDetails_notFound_redirects() throws Exception {
        mvc.perform(get("/client/orders/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}

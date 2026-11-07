package com.lombard.controller;

import com.lombard.model.Client;
import com.lombard.model.Order;
import com.lombard.model.User;
import com.lombard.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты страниц заказов клиента
@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void ordersList_opens() throws Exception {
        User u = new User(); u.setId(1L);
        com.lombard.model.Client c = new com.lombard.model.Client(); c.setId(1L);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);

        mvc.perform(get("/client/orders")).andExpect(status().isOk());
    }

    // чужой заказ смотреть нельзя
    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void orderDetails_otherClientOrder_redirects() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Client other = new Client(); other.setId(99L);
        Order o = new Order(); o.setId(5L); o.setClient(other);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(orderService.findById(5L)).thenReturn(o);

        mvc.perform(get("/client/orders/5")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void orderDetails_ownOrder_opens() throws Exception {
        User u = new User(); u.setId(1L);
        Client c = new Client(); c.setId(1L);
        Order o = new Order(); o.setId(5L); o.setClient(c);
        when(userService.findByUsername("user")).thenReturn(u);
        when(clientService.findByUserId(1L)).thenReturn(c);
        when(orderService.findById(5L)).thenReturn(o);

        mvc.perform(get("/client/orders/5")).andExpect(status().isOk());
    }

}

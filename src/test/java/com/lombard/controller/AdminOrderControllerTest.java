package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Client;
import com.lombard.model.Order;
import com.lombard.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты заказов в админке: список с фильтрами карточка заказа и смена статуса
@WebMvcTest(controllers = AdminOrderController.class)
class AdminOrderControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "ADMIN")
    void orderDetails_existing_opens() throws Exception {
        Order o = new Order(); o.setId(1L);
        when(orderService.findById(1L)).thenReturn(o);
        mvc.perform(get("/admin/orders/1")).andExpect(status().isOk());
    }

    // при статусе «Собран» клиенту уходит уведомление
    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_assembled_notifiesClient() throws Exception {
        Order o = new Order(); o.setId(1L); o.setStatus("Оплачен");
        Client c = new Client(); c.setId(1L);
        o.setClient(c);
        when(orderService.findById(1L)).thenReturn(o);

        mvc.perform(post("/admin/orders/1/status").with(csrf()).param("status", "Собран"))
                .andExpect(status().is3xxRedirection());

        verify(orderService).save(o);
        verify(notificationService).notify(eq(c), eq("ORDER_ASSEMBLED"), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_orderNotFound_redirects() throws Exception {
        when(orderService.findById(1L)).thenReturn(null);
        mvc.perform(post("/admin/orders/1/status").with(csrf()).param("status", "Собран"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersList_opens() throws Exception {
        mvc.perform(get("/admin/orders")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersList_withFilters_opens() throws Exception {
        when(orderService.findAll()).thenReturn(List.of());
        mvc.perform(get("/admin/orders")
                        .param("status", "Оплачен")
                        .param("delivery", "DELIVERY")
                        .param("sort", "total")
                        .param("query", "test"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void orderDetails_notFound_redirects() throws Exception {
        when(orderService.findById(1L)).thenReturn(null);
        mvc.perform(get("/admin/orders/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_unknownStatus_redirects() throws Exception {
        Order o = new Order(); o.setId(1L); o.setStatus("Оплачен");
        when(orderService.findById(1L)).thenReturn(o);
        mvc.perform(post("/admin/orders/1/status").with(csrf())
                        .param("status", "INVALID"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersList_filterByStatusAndDelivery_opens() throws Exception {
        com.lombard.model.Order o = new com.lombard.model.Order();
        o.setOrderNumber("ORD-1"); o.setStatus("Собран"); o.setDeliveryType("PICKUP");
        o.setCreatedAt(java.time.LocalDateTime.now());
        Client c = new Client(); c.setFullName("Иван"); o.setClient(c);
        when(orderService.findAll()).thenReturn(List.of(o));

        mvc.perform(get("/admin/orders")
                .param("status", "Собран").param("delivery", "PICKUP")
                .param("sort", "number").param("query", "Иван")).andExpect(status().isOk());
    }

    // Собран Доставлен Отменен - у каждого свое уведомление
    @Test
    @WithMockUser(roles = "ADMIN")
    void changeStatus_eachStatus_redirects() throws Exception {
        com.lombard.model.Order o = new com.lombard.model.Order(); o.setId(1L); o.setClient(new Client());
        when(orderService.findById(1L)).thenReturn(o);

        mvc.perform(post("/admin/orders/1/status").with(csrf()).param("status", "Собран")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/orders/1/status").with(csrf()).param("status", "Доставлен")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/admin/orders/1/status").with(csrf()).param("status", "Отменён")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersList_sortAndSearch_opens() throws Exception {
        Order o1 = new Order(); o1.setOrderNumber("ORD-1"); o1.setStatus("Собран"); o1.setDeliveryType("PICKUP");
        o1.setCreatedAt(LocalDateTime.now()); o1.setTotalAmount(BigDecimal.TEN);
        Client c = new Client(); c.setFullName("Иван"); o1.setClient(c);

        when(orderService.findAll()).thenReturn(List.of(o1));

        mvc.perform(get("/admin/orders").param("sort", "number")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "client")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "total")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "date_asc")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("query", "Иван")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersList_allSortOptions_open() throws Exception {
        Order o = new Order(); o.setId(1L); o.setOrderNumber("1");
        o.setTotalAmount(java.math.BigDecimal.TEN);
        o.setCreatedAt(java.time.LocalDateTime.now());
        o.setClient(new com.lombard.model.Client());
        o.getClient().setFullName("Test");

        when(orderService.findAll()).thenReturn(List.of(o));

        mvc.perform(get("/admin/orders").param("sort", "number")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "client")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "total")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "date_asc")).andExpect(status().isOk());
        mvc.perform(get("/admin/orders").param("sort", "default")).andExpect(status().isOk());
    }

    // сервис может вернуть неизменяемый список (List.of), страница всё равно должна открыться,
    // а не уйти на страницу ошибки
    @Test
    @WithMockUser(roles = "ADMIN")
    void ordersList_immutableListFromService_opensOrdersPage() throws Exception {
        Order o = new Order();
        o.setOrderNumber("O1");
        o.setStatus("Оплачен");
        o.setDeliveryType("PICKUP");
        o.setCreatedAt(LocalDateTime.now());
        when(orderService.findAll()).thenReturn(List.of(o));

        mvc.perform(get("/admin/orders").param("status", "Оплачен").param("sort", "number"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin_orders"));
    }
}

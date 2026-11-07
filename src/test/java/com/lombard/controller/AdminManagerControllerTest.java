package com.lombard.controller;

import com.lombard.model.User;
import com.lombard.service.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты страниц менеджеров в админке: список и профиль со статистикой
@WebMvcTest(controllers = AdminManagerController.class)
class AdminManagerControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "ADMIN")
    void managersList_empty_opens() throws Exception {
        when(userService.findByRole("MANAGER")).thenReturn(List.of());
        mvc.perform(get("/admin/managers")).andExpect(status().isOk());
    }

    // профиль открывается только для пользователя с ролью MANAGER
    @Test
    @WithMockUser(roles = "ADMIN")
    void managerProfile_notManager_redirects() throws Exception {
        User u = new User("c", "p", "CLIENT"); u.setId(1L);
        when(userService.findById(1L)).thenReturn(u);
        mvc.perform(get("/admin/managers/1")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void managerProfile_notFound_redirects() throws Exception {
        when(userService.findById(1L)).thenReturn(null);
        mvc.perform(get("/admin/managers/1")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void managerProfile_noActions_opens() throws Exception {
        User u = new User(); u.setId(1L); u.setUsername("mgr"); u.setRole("MANAGER");
        when(userService.findById(1L)).thenReturn(u);

        Map<String, Object> emptyStats = new HashMap<>();
        emptyStats.put("total", 0L);
        emptyStats.put("approved", 0L);
        emptyStats.put("rejected", 0L);
        emptyStats.put("issued", 0L);
        emptyStats.put("closed", 0L);
        emptyStats.put("extended", 0L);
        emptyStats.put("sold", 0L);

        when(managerStatsService.getStatsThisMonth("mgr")).thenReturn(emptyStats);
        when(managerStatsService.getStatsAllTime("mgr")).thenReturn(emptyStats);
        when(managerStatsService.getAllActions("mgr")).thenReturn(java.util.List.of());

        mvc.perform(get("/admin/managers/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void managersList_withManager_opensListAndProfile() throws Exception {
        User m = new User(); m.setId(1L); m.setRole("MANAGER"); m.setUsername("manager");
        when(userService.findByRole("MANAGER")).thenReturn(List.of(m));

        mvc.perform(get("/admin/managers")).andExpect(status().isOk());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 0);
        stats.put("approved", 0);
        stats.put("rejected", 0);
        stats.put("issued", 0);
        stats.put("closed", 0);
        stats.put("extended", 0);
        stats.put("sold", 0);

        when(userService.findById(1L)).thenReturn(m);
        when(managerStatsService.getStatsThisMonth("manager")).thenReturn(stats);
        when(managerStatsService.getStatsAllTime("manager")).thenReturn(stats);
        when(managerStatsService.getAllActions("manager")).thenReturn(new ArrayList<>());
        mvc.perform(get("/admin/managers/1")).andExpect(status().isOk());

        m.setRole("CLIENT");
        mvc.perform(get("/admin/managers/1")).andExpect(status().is3xxRedirection());

        when(userService.findById(2L)).thenReturn(null);
        mvc.perform(get("/admin/managers/2")).andExpect(status().is3xxRedirection());
    }

}

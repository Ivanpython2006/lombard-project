package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.service.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты админки: список пользователей создание смена роли удаление
// импорт из CSV и страница статистики
@WebMvcTest(controllers = AdminController.class)
class AdminControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    void deleteUser_otherUser_redirects() throws Exception {
        mvc.perform(post("/admin/users/2/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usersList_withSearch_opens() throws Exception {
        when(userService.findAll()).thenReturn(List.of(new User()));
        mvc.perform(get("/admin/users")
                        .param("query", "test")
                        .param("sort", "asc"))
                .andExpect(status().isOk());
    }

    // менеджеру удалять пользователей нельзя
    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteUser_asManager_forbidden() throws Exception {
        mvc.perform(post("/admin/users/2/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importUsers_emptyFile_redirects() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", new byte[0]);
        mvc.perform(multipart("/admin/users/import").file(f).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    // в файле две нормальные строки => две регистрации
    @Test
    @WithMockUser(roles = "ADMIN")
    void importUsers_validCsv_registersEachRow() throws Exception {
        String csv = "username;password;role\nbob;123456;MANAGER\nalice;123456;CLIENT\n";
        MockMultipartFile f = new MockMultipartFile("file", "u.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        when(userService.register(any(), any(), any())).thenReturn(new com.lombard.model.User());

        mvc.perform(multipart("/admin/users/import").file(f).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(userService, times(2)).register(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void importUsers_badRows_skipsThem() throws Exception {
        String csv = "username;password;role\nbob;12;MANAGER\nbadrow\ncarol;123456;BOSS\n";
        MockMultipartFile f = new MockMultipartFile("file", "u.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/admin/users/import").file(f).with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(userService, never()).register(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void stats_withLoans_opens() throws Exception {
        Loan l1 = new Loan(); l1.setAmount(new BigDecimal("1000")); l1.setStatus("Выдан");
        l1.setCreatedDate(LocalDate.now().minusMonths(2));
        l1.setDueDate(LocalDate.now().plusMonths(1));
        l1.setCollaterals(new ArrayList<>());
        Loan l2 = new Loan(); l2.setAmount(new BigDecimal("2000")); l2.setStatus("Закрыт");
        l2.setCreatedDate(LocalDate.now().minusMonths(1));
        l2.setDueDate(LocalDate.now());
        l2.setCollaterals(new ArrayList<>());
        Loan l3 = new Loan(); l3.setAmount(BigDecimal.TEN); l3.setStatus("Просрочен");
        l3.setCollaterals(new ArrayList<>());

        when(userService.count()).thenReturn(5L);
        when(clientService.count()).thenReturn(3L);
        when(loanService.count()).thenReturn(3L);
        when(loanService.getAverageLoanAmount()).thenReturn(BigDecimal.TEN);
        when(loanService.getAverageWaitingDays()).thenReturn(2.5);
        when(loanService.countByStatus()).thenReturn(Map.of("Выдан", 1L, "Закрыт", 1L));
        when(loanService.sumByStatus()).thenReturn(Map.of("Выдан", BigDecimal.TEN));
        when(loanService.findAll()).thenReturn(List.of(l1, l2, l3));

        mvc.perform(get("/admin/stats")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void stats_noData_opens() throws Exception {
        when(userService.count()).thenReturn(0L);
        when(clientService.count()).thenReturn(0L);
        when(loanService.count()).thenReturn(0L);
        when(loanService.getAverageLoanAmount()).thenReturn(BigDecimal.ZERO);
        when(loanService.getAverageWaitingDays()).thenReturn(0.0);
        when(loanService.countByStatus()).thenReturn(Map.of());
        when(loanService.sumByStatus()).thenReturn(Map.of());
        when(loanService.findAll()).thenReturn(List.of());

        mvc.perform(get("/admin/stats")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usersPage_opens() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usersList_filterByRoleAndSort_opens() throws Exception {
        when(userService.findAll()).thenReturn(List.of());
        mvc.perform(get("/admin/users")
                        .param("query", "admin")
                        .param("role", "ADMIN")
                        .param("sort", "username")
                        .param("page", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void newUserForm_opens() throws Exception {
        mvc.perform(get("/admin/users/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_unknownRole_notCreated() throws Exception {
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "u")
                        .param("password", "123456")
                        .param("role", "BOSS"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    // админ не может удалить сам себя
    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    void deleteUser_himself_showsError() throws Exception {
        User u = new User("admin", "p", "ADMIN"); u.setId(1L);
        when(userService.findById(1L)).thenReturn(u);
        doThrow(new IllegalStateException("Нельзя удалить самого себя"))
                .when(userService).delete(1L, "admin");
        mvc.perform(post("/admin/users/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    void changeRole_himself_forbidden() throws Exception {
        User u = new User("admin", "p", "ADMIN"); u.setId(1L);
        when(userService.findById(1L)).thenReturn(u);
        mvc.perform(post("/admin/users/1/role").with(csrf())
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    void changeRole_otherUser_changesRole() throws Exception {
        User u = new User("bob", "p", "CLIENT"); u.setId(2L);
        when(userService.findById(2L)).thenReturn(u);
        doNothing().when(userService).changeRole(2L, "MANAGER");

        mvc.perform(post("/admin/users/2/role").with(csrf()).param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
        verify(userService).changeRole(2L, "MANAGER");
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    void changeRole_unknownRole_notChanged() throws Exception {
        mvc.perform(post("/admin/users/2/role").with(csrf()).param("role", "BOSS"))
                .andExpect(status().is3xxRedirection());
        verify(userService, never()).changeRole(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin")
    void changeRole_userNotFound_redirects() throws Exception {
        when(userService.findById(2L)).thenReturn(null);
        mvc.perform(post("/admin/users/2/role").with(csrf()).param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_validData_registers() throws Exception {
        User created = new User("bob", "enc", "MANAGER"); created.setId(2L);
        when(userService.register("bob", "123456", "MANAGER")).thenReturn(created);
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "bob")
                        .param("password", "123456")
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_takenUsername_returnsForm() throws Exception {
        when(userService.register(any(), any(), any()))
                .thenThrow(new RuntimeException("Логин уже занят"));
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "bob")
                        .param("password", "123456")
                        .param("role", "MANAGER"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void exportUsers_returnsCsv() throws Exception {
        mvc.perform(get("/admin/users/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_badData_returnsForm() throws Exception {
        mvc.perform(post("/admin/users/new").with(csrf())
                        .param("username", "")
                        .param("password", "123")
                        .param("role", "MANAGER"))
                .andExpect(status().is3xxRedirection());
    }

    // займы в разных статусах чтобы отработали все фильтры и подсчеты на странице статистики
    @Test
    @WithMockUser(roles = "ADMIN")
    void stats_loansInDifferentStatuses_calculatesTotals() throws Exception {
        Loan l1 = new Loan();
        l1.setAmount(new BigDecimal("1000"));
        l1.setStatus("Выдан");
        l1.setCreatedDate(LocalDate.now().minusDays(10));
        l1.setDueDate(LocalDate.now().plusDays(20));
        Collateral c1 = new Collateral(); c1.setEstimatedPrice(new BigDecimal("500"));
        l1.setCollaterals(new ArrayList<>(List.of(c1)));

        Loan l2 = new Loan();
        l2.setAmount(new BigDecimal("2000"));
        l2.setStatus("Закрыт");
        l2.setCollaterals(new ArrayList<>());

        Loan l3 = new Loan();
        l3.setAmount(new BigDecimal("3000"));
        l3.setStatus("Просрочен");
        l3.setCollaterals(new ArrayList<>());

        Loan l4 = new Loan();
        l4.setAmount(new BigDecimal("4000"));
        l4.setStatus("На продаже");
        l4.setCollaterals(new ArrayList<>());

        when(loanService.findAll()).thenReturn(List.of(l1, l2, l3, l4));
        when(loanService.countByStatus()).thenReturn(Map.of("Выдан", 1L));
        when(loanService.sumByStatus()).thenReturn(Map.of("Выдан", BigDecimal.TEN));
        when(userService.count()).thenReturn(5L);
        when(clientService.count()).thenReturn(3L);
        when(loanService.count()).thenReturn(4L);

        mvc.perform(get("/admin/stats")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void usersList_allSortOptions_open() throws Exception {
        User u1 = new User(); u1.setId(1L); u1.setUsername("test"); u1.setUserNumber("A1"); u1.setRole("ADMIN");
        User u2 = new User(); u2.setId(2L); u2.setUsername("other"); u2.setUserNumber("A2"); u2.setRole("CLIENT");
        when(userService.findAll()).thenReturn(List.of(u1, u2));

        mvc.perform(get("/admin/users").param("sort", "number")).andExpect(status().isOk());
        mvc.perform(get("/admin/users").param("sort", "username")).andExpect(status().isOk());
        mvc.perform(get("/admin/users").param("sort", "role")).andExpect(status().isOk());
        mvc.perform(get("/admin/users").param("query", "test").param("role", "ADMIN")).andExpect(status().isOk());
    }

}

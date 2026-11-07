package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Client;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.service.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//Тесты работы менеджера с займами: список карточка одобрение заявки редактирование смена статуса продление и выгрузка

@WebMvcTest(controllers = LoanController.class)
class LoanControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void export_noLoans_returnsCsv() throws Exception {
        when(loanService.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/loans/export")).andExpect(status().isOk());
    }

    // ставка больше 100% или оценка 0 - заявку не одобряем
    @Test
    @WithMockUser(roles = "MANAGER")
    void approve_badRateOrPrice_redirects() throws Exception {
        mvc.perform(post("/loans/1/approve").with(csrf())
                        .param("rate", "150")
                        .param("estimatedPrice", "1000")
                        .param("approvedAmount", "800"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/loans/1/approve").with(csrf())
                        .param("rate", "10")
                        .param("estimatedPrice", "0")
                        .param("approvedAmount", "800"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void edit_withDateAndBadDate_redirects() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("В ожидании");
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/edit").with(csrf())
                        .param("amount", "1000")
                        .param("rate", "10")
                        .param("status", "Одобрен")
                        .param("dueDate", "2026-12-31"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/loans/1/edit").with(csrf())
                        .param("amount", "1000")
                        .param("dueDate", "invalid-date"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void export_withLoan_returnsCsv() throws Exception {
        Loan l = new Loan(); l.setId(1L);
        l.setAmount(BigDecimal.TEN); l.setRate(BigDecimal.ONE); l.setStatus("Выдан");
        l.setCreatedDate(LocalDate.now()); l.setDueDate(LocalDate.now().plusDays(10));

        Client c = new Client(); c.setFullName("Test"); l.setClient(c);
        Collateral col = new Collateral(); col.setName("Phone"); col.setEstimatedPrice(BigDecimal.TEN);
        l.setCollaterals(List.of(col));

        when(loanService.findAll()).thenReturn(List.of(l));

        mvc.perform(get("/loans/export")).andExpect(status().isOk());
    }

    // кривую дату просто игнорю остальное сохраняется
    @Test
    @WithMockUser(roles = "MANAGER")
    void edit_badDate_savesWithoutDate() throws Exception {
        Loan l = new Loan(); l.setId(1L);
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/edit").with(csrf())
                        .param("amount", "1000")
                        .param("rate", "10")
                        .param("dueDate", "invalid-date"))
                .andExpect(status().is3xxRedirection());

        verify(loanService).save(l);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void loansList_opens() throws Exception {
        mvc.perform(get("/loans")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void loanDetails_existing_opens() throws Exception {
        Loan l = new Loan(); l.setId(1L);
        Collateral c = new Collateral();
        c.setPhotoUrls(List.of("/uploads/test.jpg"));
        l.setCollaterals(new ArrayList<>(List.of(c)));

        when(loanService.findById(1L)).thenReturn(l);
        mvc.perform(get("/loans/1")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void changeStatus_pendingToApproved_saves() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("В ожидании");
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/status").with(csrf()).param("status", "Одобрен"))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(l);
    }

    // из «Закрыт» обратно в «Выдан» перейти нельзя
    @Test
    @WithMockUser(roles = "MANAGER")
    void changeStatus_closedToIssued_notAllowed() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("Закрыт");
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/status").with(csrf()).param("status", "Выдан"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void extend_issuedLoan_callsService() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("Выдан");
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/extend").with(csrf()).param("months", "3"))
                .andExpect(status().is3xxRedirection());

        verify(loanService).extendLoan(eq(1L), eq(3));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void edit_validData_saves() throws Exception {
        Loan l = new Loan(); l.setId(1L);
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/edit").with(csrf())
                        .param("rate", "10")
                        .param("estimatedPrice", "1000")
                        .param("approvedAmount", "500")
                        .param("dueDate", "2026-12-31"))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(l);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void changeStatus_confirmedToIssued_saves() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("Ожидает подтверждения"); l.setClient(new Client());
        l.setAmount(BigDecimal.TEN); l.setRate(BigDecimal.ONE);

        when(loanService.findById(1L)).thenReturn(l);
        when(loanService.getItemName(l)).thenReturn("Phone");

        mvc.perform(post("/loans/1/status").with(csrf()).param("status", "Выдан"))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(l);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void approve_validData_saves() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("В ожидании"); l.setClient(new com.lombard.model.Client());
        l.setAmount(BigDecimal.TEN); l.setRate(BigDecimal.ONE);
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/loans/1/approve").with(csrf())
                        .param("rate", "10")
                        .param("estimatedPrice", "1000")
                        .param("approvedAmount", "800"))
                .andExpect(status().is3xxRedirection());
        verify(loanService, atLeastOnce()).save(l);
    }
}

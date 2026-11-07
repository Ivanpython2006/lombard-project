package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты работы с займами: одобрение статусы продление отзыв удаление. Все проверки на стандартном наборе данных (заем L100 «В ожидании»)

class LoanControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void loanPages_asAdmin_open() throws Exception {
        mvc.perform(get("/loans")).andExpect(status().isOk());
        mvc.perform(get("/loans/" + loan.getId())).andExpect(status().isOk());
        mvc.perform(get("/loans/" + loan.getId() + "/approve")).andExpect(status().isOk());
    }

    // текст вместо номера займа в адресе - это страница 404, а не ошибка сервера 500
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void loanDetails_textInsteadOfId_returns404() throws Exception {
        mvc.perform(get("/loans/abc"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approve_validForm_redirects() throws Exception {
        mvc.perform(post("/loans/" + loan.getId() + "/approve").with(csrf())
                        .param("rate", "10")
                        .param("estimatedPrice", "5000")
                        .param("approvedAmount", "1000"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatus_redirects() throws Exception {
        mvc.perform(post("/loans/" + loan.getId() + "/status").with(csrf())
                        .param("status", "Одобрен"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void extend_issuedLoan_redirects() throws Exception {
        loan.setStatus("Выдан");
        loanRepository.save(loan);

        mvc.perform(post("/loans/" + loan.getId() + "/extend").with(csrf())
                        .param("months", "3"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void export_returnsCsv() throws Exception {
        mvc.perform(get("/loans/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approveThenChangeStatus_redirect() throws Exception {
        mvc.perform(post("/loans/" + loan.getId() + "/approve").with(csrf())
                .param("rate", "10")
                .param("estimatedPrice", "5000")
                .param("approvedAmount", "1000")).andExpect(status().is3xxRedirection());

        mvc.perform(post("/loans/" + loan.getId() + "/status").with(csrf())
                .param("status", "Одобрен")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void loansList_asClient_forbidden() throws Exception {
        mvc.perform(get("/loans"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approveForm_opens() throws Exception {
        mvc.perform(get("/loans/" + loan.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(view().name("loan_approve"));
    }

    // после одобрения заем ждет согласия клиента
    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    void approve_validForm_waitsForClientConfirmation() throws Exception {
        mvc.perform(post("/loans/" + loan.getId() + "/approve").with(csrf())
                        .param("rate", "15.5")
                        .param("estimatedPrice", "40000")
                        .param("approvedAmount", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        assertEquals("Ожидает подтверждения",
                loanRepository.findById(loan.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void approve_badRate_redirects() throws Exception {
        mvc.perform(post("/loans/" + loan.getId() + "/approve").with(csrf())
                        .param("rate", "150").param("estimatedPrice", "40000")
                        .param("approvedAmount", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatus_toApproved_savedToDatabase() throws Exception {
        mvc.perform(post("/loans/" + loan.getId() + "/status").with(csrf())
                        .param("status", "Одобрен"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        assertEquals("Одобрен", loanRepository.findById(loan.getId()).orElseThrow().getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatus_fromClosed_notAllowed() throws Exception {
        loan.setStatus("Закрыт");
        loanRepository.save(loan);
        mvc.perform(post("/loans/" + loan.getId() + "/status").with(csrf())
                        .param("status", "Выдан"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void extend_threeMonths_dueDateMoved() throws Exception {
        loan.setStatus("Выдан");
        loanRepository.save(loan);
        LocalDate oldDue = loan.getDueDate();
        mvc.perform(post("/loans/" + loan.getId() + "/extend").with(csrf())
                        .param("months", "3"))
                .andExpect(status().is3xxRedirection());
        assertEquals(oldDue.plusMonths(3),
                loanRepository.findById(loan.getId()).orElseThrow().getDueDate());
    }

    // менеджер отзывает свое предложение до подтверждения клиентом
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void withdrawOffer_redirects() throws Exception {
        loan.setStatus("Ожидает подтверждения");
        loanRepository.save(loan);
        mvc.perform(post("/loans/" + loan.getId() + "/withdraw").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void loanDetails_opens() throws Exception {
        mvc.perform(get("/loans/" + loan.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("loan_details"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void delete_loan_removedFromDatabase() throws Exception {
        collateralRepository.deleteAll();

        mvc.perform(post("/loans/" + loan.getId() + "/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertEquals(0, loanRepository.count());
    }

}

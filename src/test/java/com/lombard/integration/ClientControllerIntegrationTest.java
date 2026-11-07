package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//Интеграционные тесты личного кабинета клиента на стандартном наборе данных

class ClientControllerIntegrationTest extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        createDefaultData();
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void clientPages_open() throws Exception {
        mvc.perform(get("/client/profile")).andExpect(status().isOk());
        mvc.perform(get("/client/loans")).andExpect(status().isOk());
        mvc.perform(get("/client/loans/new")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void createLoan_validForm_redirects() throws Exception {
        mvc.perform(post("/client/loans/new").with(csrf())
                        .param("amount", "1000")
                        .param("collateralName", "Вещь")
                        .param("collateralDescription", "Описание")
                        .param("months", "3"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void updateProfile_redirects() throws Exception {
        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Новиков")
                        .param("firstName", "Николай")
                        .param("email", "new@test.com")
                        .param("phone", "+79990001122"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void updateProfile_newPhone_redirects() throws Exception {
        mvc.perform(post("/client/profile").with(csrf())
                        .param("lastName", "Новиков")
                        .param("firstName", "Николай")
                        .param("email", "new@test.com")
                        .param("phone", "+79990001122"))
                .andExpect(status().is3xxRedirection());
    }

    //весь путь клиента: заявка пополнение подтверждение и погашение
    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void loanFlow_createDepositConfirmRepay_redirects() throws Exception {
        mvc.perform(post("/client/loans/new").with(csrf())
                .param("amount", "1000")
                .param("collateralName", "Товар")
                .param("collateralDescription", "Описание")
                .param("months", "3")).andExpect(status().is3xxRedirection());

        mvc.perform(post("/client/account/deposit").with(csrf()).param("amount", "5000"))
                .andExpect(status().is3xxRedirection());

        loan.setStatus("Ожидает подтверждения");
        loanRepository.save(loan);
        mvc.perform(post("/client/loans/" + loan.getId() + "/confirm").with(csrf())
                .param("agree", "true")).andExpect(status().is3xxRedirection());
    }
}

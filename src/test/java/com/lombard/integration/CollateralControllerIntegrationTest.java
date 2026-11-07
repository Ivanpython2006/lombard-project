package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты раздела «Залоги»
class CollateralControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("стандартный набор данных")
    class OnDefaultData {

        @BeforeEach
        void setUp() {
            createDefaultData();
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void collateralPages_open() throws Exception {
            mvc.perform(get("/collaterals")).andExpect(status().isOk());
            mvc.perform(get("/collaterals/export")).andExpect(status().isOk());
            mvc.perform(get("/collaterals/" + loan.getCollaterals().get(0).getId() + "/edit"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("данные: клиент, заём, залог")
    class WithClientLoanCollateral {

        private Loan loan;

        private Collateral collateral;

        @BeforeEach
        void setUp() {
            collateralRepository.deleteAll();
            loanRepository.deleteAll();
            clientRepository.deleteAll();
            userRepository.deleteAll();

            User u = new User("client1", "pass", "CLIENT");
            u.setUserNumber("U_C1");
            userRepository.save(u);

            Client c = new Client("Клиент", "+79990001122");
            c.setUser(u);
            c.setClientNumber("C100");
            clientRepository.save(c);

            loan = new Loan();
            loan.setAmount(new BigDecimal("1000"));
            loan.setStatus("На складе");
            loan.setCreatedDate(LocalDate.now());
            loan.setClient(c);
            loan.setLoanNumber("L100");
            loanRepository.save(loan);

            collateral = new Collateral("Телевизор", "Samsung", new BigDecimal("40000"));
            collateral.setItemNumber("M100");
            collateral.setPhotoUrls(new ArrayList<>());
            collateral.setLoan(loan);
            collateralRepository.save(collateral);
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void list_opens() throws Exception {
            mvc.perform(get("/collaterals"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("collaterals"))
                    .andExpect(model().attributeExists("collaterals"));
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void list_withFilters_opens() throws Exception {
            mvc.perform(get("/collaterals")
                            .param("query", "Телевизор")
                            .param("status", "На складе")
                            .param("sort", "name")
                            .param("page", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void export_returnsCsv() throws Exception {
            mvc.perform(get("/collaterals/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Disposition"));
        }

        @Test
        @WithMockUser(username = "manager", roles = {"MANAGER"})
        void editForm_asManager_opens() throws Exception {
            mvc.perform(get("/collaterals/" + collateral.getId() + "/edit"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("collateral_edit"));
        }

        // клиенту раздел залогов недоступен
        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void editForm_asClient_forbidden() throws Exception {
            mvc.perform(get("/collaterals/" + collateral.getId() + "/edit"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "manager", roles = {"MANAGER"})
        void edit_newName_savedToDatabase() throws Exception {
            mvc.perform(post("/collaterals/" + collateral.getId() + "/edit").with(csrf())
                            .param("name", "Новое имя")
                            .param("description", "Новое описание")
                            .param("estimatedPrice", "45000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));

            Collateral updated = collateralRepository.findById(collateral.getId()).orElseThrow();
            assertEquals("Новое имя", updated.getName());
        }

        @Test
        @WithMockUser(username = "manager", roles = {"MANAGER"})
        void edit_emptyName_redirects() throws Exception {
            mvc.perform(post("/collaterals/" + collateral.getId() + "/edit").with(csrf())
                            .param("name", "")
                            .param("estimatedPrice", "45000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "manager", roles = {"MANAGER"})
        void edit_negativePrice_redirects() throws Exception {
            mvc.perform(post("/collaterals/" + collateral.getId() + "/edit").with(csrf())
                            .param("name", "OK")
                            .param("estimatedPrice", "-100"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }

        @Test
        @WithMockUser(username = "manager", roles = {"MANAGER"})
        void delete_asManager_removedFromDatabase() throws Exception {
            Loan l = collateral.getLoan();
            l.getCollaterals().remove(collateral);
            loanRepository.save(l);

            mvc.perform(post("/collaterals/" + collateral.getId() + "/delete").with(csrf()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));

            assertFalse(collateralRepository.existsById(collateral.getId()));
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void delete_asClient_forbidden() throws Exception {
            mvc.perform(post("/collaterals/" + collateral.getId() + "/delete").with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты магазина

class ShopControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("стандартный набор данных")
    class OnDefaultData {

        @BeforeEach
        void setUp() {
            createDefaultData();
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void shopAndCart_addItem_redirects() throws Exception {
            mvc.perform(get("/shop")).andExpect(status().isOk());
            mvc.perform(get("/client/cart")).andExpect(status().isOk());

            mvc.perform(post("/shop/" + loan.getId() + "/add-to-cart").with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void manageAndExport_open() throws Exception {
            mvc.perform(get("/shop/manage")).andExpect(status().isOk());
            mvc.perform(get("/shop/export")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("данные: клиент, заём, залог")
    class WithClientLoanCollateral {

        private Loan onSale;

        private Client client;

        @BeforeEach
        void setUp() {
            collateralRepository.deleteAll();
            loanRepository.deleteAll();
            clientRepository.deleteAll();
            userRepository.deleteAll();
            numberSequenceRepository.deleteAll();

            User u = new User("client1", passwordEncoder.encode("pass"), "CLIENT");
            u.setUserNumber("U_C1");
            userRepository.save(u);
            client = new Client("Клиент", "+79990001122");
            client.setUser(u);
            client.setClientNumber("C100");
            clientRepository.save(client);

            onSale = new Loan();
            onSale.setAmount(new BigDecimal("1000"));
            onSale.setStatus("На продаже");
            onSale.setCreatedDate(LocalDate.now());
            onSale.setClient(client);
            onSale.setLoanNumber("L200");
            loanRepository.save(onSale);

            Collateral c = new Collateral("Товар", "desc", new BigDecimal("5000"));
            c.setItemNumber("M100");
            c.setPhotoUrls(new ArrayList<>());
            c.setLoan(onSale);
            collateralRepository.save(c);
            onSale.getCollaterals().add(c);
            loanRepository.save(onSale);
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void shopPage_opens() throws Exception {
            mvc.perform(get("/shop")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void managePage_opens() throws Exception {
            mvc.perform(get("/shop/manage")).andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void removeFromSale_redirects() throws Exception {
            mvc.perform(post("/shop/" + onSale.getId() + "/remove").with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void export_returnsCsv() throws Exception {
            mvc.perform(get("/shop/export")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("права доступа")
    class Security {

        // управление магазином только для сотрудников
        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void managePage_asClient_forbidden() throws Exception {
            mvc.perform(get("/shop/manage"))
                    .andExpect(status().isForbidden());
        }

        // фото залогов без входа в систему не отдаём
        @Test
        void uploads_anonymous_redirectsToLogin() throws Exception {
            mvc.perform(get("/uploads/item_1.jpg"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }
}

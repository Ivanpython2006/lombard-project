package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.model.Account;
import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.model.User;
import com.lombard.repository.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты счёта клиента: запросы идут через MockMvc в настоящие сервисы и базу H2.

class AccountControllerIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("стандартный набор данных")
    class OnDefaultData {

        @BeforeEach
        void setUp() {
            createDefaultData();
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void depositThenWithdraw_redirects() throws Exception {
            mvc.perform(post("/client/account/deposit").with(csrf()).param("amount", "5000"))
                    .andExpect(status().is3xxRedirection());
            mvc.perform(post("/client/account/withdraw").with(csrf()).param("amount", "1000"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("данные: клиент, счёт")
    class WithClientAccount {

        private Client client;

        @BeforeEach
        void setUp() {
            notificationRepository.deleteAll();
            accountRepository.deleteAll();
            clientRepository.deleteAll();
            userRepository.deleteAll();

            User u = new User("client1", "pass", "CLIENT");
            u.setUserNumber("U_C1");
            userRepository.save(u);
            client = new Client("Клиент", "+79990001122");
            client.setUser(u);
            client.setClientNumber("C100");
            clientRepository.save(client);

            Account acc = new Account(client);
            acc.setBalance(BigDecimal.ZERO);
            accountRepository.save(acc);
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void accountPage_opens() throws Exception {
            mvc.perform(get("/client/account"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("client_account"))
                    .andExpect(model().attributeExists("client"));
        }

        // после пополнения проверяем баланс прямо в базе
        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void deposit_5000_balanceIs5000() throws Exception {
            mvc.perform(post("/client/account/deposit").with(csrf())
                            .param("amount", "5000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));
            Account a = accountRepository.findByClientId(client.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("5000").compareTo(a.getBalance()));
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void withdraw_afterDeposit_redirects() throws Exception {
            mvc.perform(post("/client/account/deposit").with(csrf()).param("amount", "10000"));
            mvc.perform(post("/client/account/withdraw").with(csrf()).param("amount", "3000"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("success"));
        }

        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void withdraw_emptyAccount_redirectsWithError() throws Exception {
            mvc.perform(post("/client/account/withdraw").with(csrf()).param("amount", "999"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        }

        // без входа в систему кабинет недоступен
        @Test
        void accountPage_anonymous_redirectsToLogin() throws Exception {
            mvc.perform(get("/client/account"))
                    .andExpect(status().is3xxRedirection());
        }
    }

    @Nested
    @DisplayName("данные: клиент, заём")
    class WithClientLoan {

        private User testUser;

        private Client testClient;

        private Loan testLoan;

        @BeforeEach
        void setUp() {
            testUser = new User();
            testUser.setUsername("mega_user");
            testUser.setPassword("password");
            testUser.setRole("CLIENT");
            testUser.setUserNumber("U_TEST");
            userRepository.save(testUser);

            testClient = new Client();
            testClient.setFullName("Тест Тестов");
            testClient.setPhone("+79999999999");
            testClient.setUser(testUser);
            clientRepository.save(testClient);

            testLoan = new Loan();
            testLoan.setAmount(java.math.BigDecimal.TEN);
            testLoan.setStatus("В ожидании");
            testLoan.setClient(testClient);
            loanRepository.save(testLoan);
        }

        @Test
        @WithMockUser(username = "mega_user", roles = {"CLIENT"})
        void depositAndCancelLoan_redirect() throws Exception {
            mvc.perform(post("/client/account/deposit").with(csrf())
                    .param("amount", "500")).andExpect(status().is3xxRedirection());

            mvc.perform(post("/client/loans/" + testLoan.getId() + "/cancel").with(csrf()))
                    .andExpect(status().is3xxRedirection());
        }
    }
}

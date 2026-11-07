package com.lombard.integration;

import com.lombard.model.Client;
import com.lombard.model.Loan;
import com.lombard.model.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Смоук-тесты: проверяем, что все основные страницы вообще открываются (код 200) под разными ролями и ничего не падает при отрисовке шаблонов.
class SmokeIntegrationTest extends IntegrationTestBase {

    @Nested
    @DisplayName("страницы на стандартном наборе данных")
    class Pages {

        @BeforeEach
        void setUp() {
            createDefaultData();
        }

        // страницы которые видны всем
        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void publicPages_anyUser_open() throws Exception {
            mvc.perform(get("/")).andExpect(status().isOk());
            mvc.perform(get("/about")).andExpect(status().isOk());
            mvc.perform(get("/support")).andExpect(status().isOk());
            mvc.perform(get("/shop")).andExpect(status().isOk());
        }

        // весь личный кабинет клиента
        @Test
        @WithMockUser(username = "client1", roles = {"CLIENT"})
        void clientPages_asClient_open() throws Exception {
            mvc.perform(get("/client/profile")).andExpect(status().isOk());
            mvc.perform(get("/client/loans")).andExpect(status().isOk());
            mvc.perform(get("/client/loans/new")).andExpect(status().isOk());
            mvc.perform(get("/client/orders")).andExpect(status().isOk());
            mvc.perform(get("/client/orders/" + order.getId())).andExpect(status().isOk());
            mvc.perform(get("/client/cart")).andExpect(status().isOk());
            mvc.perform(get("/client/notifications")).andExpect(status().isOk());
            mvc.perform(get("/client/account")).andExpect(status().isOk());
        }

        // админка: пользователи статистика заказы склад приемка менеджеры
        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void adminPages_asAdmin_open() throws Exception {
            mvc.perform(get("/admin/users")).andExpect(status().isOk());
            mvc.perform(get("/admin/users/new")).andExpect(status().isOk());
            mvc.perform(get("/admin/users/export")).andExpect(status().isOk());
            mvc.perform(get("/admin/stats")).andExpect(status().isOk());
            mvc.perform(get("/admin/orders")).andExpect(status().isOk());
            mvc.perform(get("/admin/orders/" + order.getId())).andExpect(status().isOk());
            mvc.perform(get("/admin/warehouse")).andExpect(status().isOk());
            mvc.perform(get("/admin/intake")).andExpect(status().isOk());
            mvc.perform(get("/admin/managers")).andExpect(status().isOk());
            mvc.perform(get("/admin/notifications")).andExpect(status().isOk());
        }

        // рабочие страницы менеджера и выгрузки в CSV
        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN", "MANAGER"})
        void managerPages_asManager_open() throws Exception {
            mvc.perform(get("/loans")).andExpect(status().isOk());
            mvc.perform(get("/collaterals")).andExpect(status().isOk());
            mvc.perform(get("/collaterals/export")).andExpect(status().isOk());
            mvc.perform(get("/shop/manage")).andExpect(status().isOk());
            mvc.perform(get("/shop/export")).andExpect(status().isOk());
            mvc.perform(get("/admin/warehouse/export")).andExpect(status().isOk());
            mvc.perform(get("/client/search")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("новый клиент без счёта и без номера займа")
    class NewClient {

        private Loan newLoan;

        @BeforeEach
        void setUp() {
            User u = new User();
            u.setUsername("test_client");
            u.setPassword("pass");
            u.setRole("CLIENT");
            u.setUserNumber("U999");
            userRepository.save(u);

            Client c = new Client();
            c.setFullName("Иван Иванов");
            c.setPhone("+79990000001");
            c.setUser(u);
            clientRepository.save(c);

            newLoan = new Loan();
            newLoan.setAmount(new BigDecimal("1000"));
            newLoan.setStatus("В ожидании");
            newLoan.setClient(c);
            loanRepository.save(newLoan);
        }

        // у клиента ещё нет счета он должен создаться сам при заходе в кабинет
        @Test
        @WithMockUser(username = "test_client", roles = {"CLIENT"})
        void clientFlow_newClient_createsLoanAndDeposits() throws Exception {
            mvc.perform(get("/")).andExpect(status().isOk());
            mvc.perform(get("/client/loans")).andExpect(status().isOk());
            mvc.perform(get("/client/account")).andExpect(status().isOk());
            mvc.perform(get("/client/notifications")).andExpect(status().isOk());
            mvc.perform(get("/client/profile")).andExpect(status().isOk());

            mvc.perform(post("/client/loans/new").with(csrf())
                            .param("amount", "5000")
                            .param("months", "6")
                            .param("collateralName", "Телефон")
                            .param("collateralDescription", "Хороший"))
                    .andExpect(status().is3xxRedirection());

            mvc.perform(post("/client/account/deposit").with(csrf())
                            .param("amount", "10000"))
                    .andExpect(status().is3xxRedirection());
        }

        // менеджер одобряет заявку и смотрит склад магазин и клиентов
        @Test
        @WithMockUser(username = "manager", roles = {"MANAGER", "ADMIN"})
        void managerFlow_pendingLoan_approvesAndOpensPages() throws Exception {
            mvc.perform(get("/loans")).andExpect(status().isOk());
            mvc.perform(post("/loans/" + newLoan.getId() + "/approve").with(csrf())
                            .param("rate", "10")
                            .param("estimatedPrice", "1000")
                            .param("approvedAmount", "800"))
                    .andExpect(status().is3xxRedirection());
            mvc.perform(get("/admin/warehouse")).andExpect(status().isOk());
            mvc.perform(get("/admin/stats")).andExpect(status().isOk());
            mvc.perform(get("/shop/manage")).andExpect(status().isOk());
            mvc.perform(get("/client/search")).andExpect(status().isOk());
        }
    }
}

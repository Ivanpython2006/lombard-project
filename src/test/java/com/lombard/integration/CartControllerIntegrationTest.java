package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты корзины и оформления заказа

class CartControllerIntegrationTest extends IntegrationTestBase {

    private Client client;

    private Loan onSale;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        cartItemRepository.deleteAll();
        accountTransactionRepository.deleteAll();
        accountRepository.deleteAll();
        orderRepository.deleteAll();
        notificationRepository.deleteAll();
        loanHistoryRepository.deleteAll();
        collateralRepository.deleteAll();
        loanRepository.deleteAll();
        clientRepository.deleteAll();
        userRepository.deleteAll();
        numberSequenceRepository.deleteAll();

        User u = new User("client1", "pass", "CLIENT");
        userRepository.save(u);
        client = new Client("Клиент", "+79990001122");
        client.setUser(u);
        client.setClientNumber("C100");
        clientRepository.save(client);

        Account acc = new Account(client);
        acc.setBalance(new BigDecimal("100000"));
        accountRepository.save(acc);

        onSale = new Loan();
        onSale.setAmount(new BigDecimal("1000"));
        onSale.setStatus("На продаже");
        onSale.setCreatedDate(LocalDate.now());
        onSale.setDueDate(LocalDate.now().plusMonths(1));
        onSale.setClient(client);
        onSale.setLoanNumber("L400");
        loanRepository.save(onSale);

        Collateral c = new Collateral("Товар", "desc", new BigDecimal("5000"));
        c.setItemNumber("M400");
        c.setPhotoUrls(new ArrayList<>());
        c.setLoan(onSale);
        collateralRepository.save(c);
        onSale.getCollaterals().add(c);
        loanRepository.save(onSale);
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void cartPage_opens() throws Exception {
        mvc.perform(get("/client/cart"))
                .andExpect(status().isOk())
                .andExpect(view().name("client_cart"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void addToCart_itemOnSale_redirects() throws Exception {
        mvc.perform(post("/client/cart/add/" + onSale.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void removeFromCart_itemDeletedFromDatabase() throws Exception {
        CartItem ci = new CartItem(client, onSale);
        cartItemRepository.save(ci);

        mvc.perform(post("/client/cart/remove/" + ci.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));
        assertEquals(0, cartItemRepository.count());
    }

    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void checkout_emptyCart_redirects() throws Exception {
        mvc.perform(post("/client/cart/checkout").with(csrf())
                        .param("deliveryType", "PICKUP")
                        .param("contactPhone", "+79990001122"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }

    // после оплаты появляется заказ а корзина очищается
    @Test
    @WithMockUser(username = "client1", roles = {"CLIENT"})
    void checkout_withItem_createsOrderAndClearsCart() throws Exception {
        cartItemRepository.save(new CartItem(client, onSale));

        mvc.perform(post("/client/cart/checkout").with(csrf())
                        .param("deliveryType", "PICKUP")
                        .param("contactPhone", "+79990001122"))
                .andExpect(status().is3xxRedirection());
        assertEquals(1, orderRepository.count());
        assertEquals(0, cartItemRepository.count());
    }
}

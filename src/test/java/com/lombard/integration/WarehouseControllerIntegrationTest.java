package com.lombard.integration;

import com.lombard.model.*;
import com.lombard.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Интеграционные тесты склада.

class WarehouseControllerIntegrationTest extends IntegrationTestBase {

    private Loan inStock;

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
        Client c = new Client("Клиент", "+79990001122");
        c.setUser(u);
        c.setClientNumber("C100");
        clientRepository.save(c);

        inStock = new Loan();
        inStock.setAmount(new BigDecimal("1000"));
        inStock.setStatus("На складе");
        inStock.setCreatedDate(LocalDate.now());
        inStock.setClient(c);
        inStock.setLoanNumber("L300");
        loanRepository.save(inStock);

        Collateral col = new Collateral("Товар", "desc", new BigDecimal("5000"));
        col.setItemNumber("M300");
        col.setPhotoUrls(new ArrayList<>());
        col.setLoan(inStock);
        collateralRepository.save(col);
        inStock.getCollaterals().add(col);
        loanRepository.save(inStock);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void warehousePage_opens() throws Exception {
        mvc.perform(get("/admin/warehouse")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void saveItem_redirects() throws Exception {
        mvc.perform(post("/admin/warehouse/" + inStock.getId() + "/save").with(csrf())
                        .param("itemName", "TV")
                        .param("price", "10000"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void publish_withPrice_redirects() throws Exception {
        mvc.perform(post("/admin/warehouse/" + inStock.getId() + "/publish").with(csrf())
                        .param("price", "10000"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void export_returnsCsv() throws Exception {
        mvc.perform(get("/admin/warehouse/export")).andExpect(status().isOk());
    }
}

package com.lombard.controller;

import com.lombard.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Страница поддержки открывается для клиента
@WebMvcTest(controllers = SupportController.class)
class SupportControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "CLIENT", username = "user")
    void supportPage_asClient_opens() throws Exception {
        mvc.perform(get("/support")).andExpect(status().isOk());
    }
}

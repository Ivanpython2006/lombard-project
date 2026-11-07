package com.lombard.controller;

import com.lombard.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Страница «О нас» открывается для вошедшего пользователя
@WebMvcTest(controllers = AboutController.class)
class AboutControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser
    void aboutPage_opens() throws Exception {
        mvc.perform(get("/about")).andExpect(status().isOk());
    }
}

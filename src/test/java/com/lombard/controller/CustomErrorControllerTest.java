package com.lombard.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//Тесты контроллера /error: по коду ошибки выбирается нужный шаблон

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomErrorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CustomErrorController errorController;

    @Test
    void handleError_status404_returns404Page() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        assertEquals("error/404", errorController.handleError(req));
    }

    @Test
    void handleError_status403_returns403Page() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        assertEquals("error/403", errorController.handleError(req));
    }

    @Test
    void handleError_status500_returnsErrorPage() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        assertEquals("error", errorController.handleError(req));
    }

    @Test
    void handleError_noStatus_returnsErrorPage() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertEquals("error", errorController.handleError(req));
    }

    @Test
    void errorUrl_opensErrorPage() throws Exception {
        mockMvc.perform(get("/error"))
                .andExpect(status().isOk());
    }
}
package com.lombard.controller;

import com.lombard.service.*;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты страницы приёмки (новые заявки на займы)
@WebMvcTest(controllers = IntakeController.class)
class IntakeControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void export_returnsCsv() throws Exception {
        when(loanService.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/admin/intake/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void intakePage_opens() throws Exception {
        mvc.perform(get("/admin/intake")).andExpect(status().isOk());
    }
}

package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Collateral;
import com.lombard.model.Loan;
import com.lombard.service.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты склада: сюда попадают вещи по просроченным займам (отсюда их выставляют в магазин)
@WebMvcTest(controllers = WarehouseController.class)
class WarehouseControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void export_returnsCsv() throws Exception {
        when(loanService.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/admin/warehouse/export")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void publish_emptyPrice_redirects() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("На складе");
        when(loanService.findById(1L)).thenReturn(l);
        mvc.perform(post("/admin/warehouse/1/publish").with(csrf()).param("price", ""))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void saveItem_withPhotos_appendsPhotos() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("На складе");
        Collateral c = new Collateral(); c.setId(1L);
        l.setCollaterals(new ArrayList<>(List.of(c)));

        when(loanService.findById(1L)).thenReturn(l);

        MockMultipartFile photo = new MockMultipartFile("photos", "test.jpg", "image/jpeg", "data".getBytes());

        mvc.perform(multipart("/admin/warehouse/1/save").file(photo).with(csrf())
                        .param("itemName", "Phone")
                        .param("price", "1000"))
                .andExpect(status().is3xxRedirection());

        verify(collateralService).appendPhotos(eq(c), any(), anyString());
        verify(loanService).save(l);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void publish_withPrice_saves() throws Exception {
        Loan l = new Loan(); l.setId(1L); l.setStatus("На складе");
        l.setCollaterals(new ArrayList<>());
        when(loanService.findById(1L)).thenReturn(l);

        mvc.perform(post("/admin/warehouse/1/publish").with(csrf()).param("price", "1000"))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(l);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void saveItem_validData_saves() throws Exception {
        when(loanService.findById(1L)).thenReturn(loanInStock());
        mvc.perform(post("/admin/warehouse/1/save").with(csrf())
                        .param("itemName", "Phone")
                        .param("itemDescription", "good")
                        .param("price", "1000"))
                .andExpect(status().is3xxRedirection());
        verify(loanService).save(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_callsService() throws Exception {
        when(loanService.findById(1L)).thenReturn(loanInStock());
        mvc.perform(post("/admin/warehouse/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(loanService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void warehousePage_opens() throws Exception {
        mvc.perform(get("/admin/warehouse")).andExpect(status().isOk());
    }

    private Loan loanInStock() {
        Loan l = new Loan();
        l.setId(1L); l.setStatus("На складе");
        Collateral c = new Collateral();
        l.setCollaterals(new ArrayList<>(List.of(c)));
        return l;
    }
}

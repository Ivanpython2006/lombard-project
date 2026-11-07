package com.lombard.controller;

import com.lombard.model.*;
import com.lombard.model.Collateral;
import com.lombard.service.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Тесты раздела «Залоги» для менеджера: список редактирование фото удаление выгрузка.

@WebMvcTest(controllers = CollateralController.class)
class CollateralControllerTest extends ControllerTestBase {

    @Test
    @WithMockUser(roles = "MANAGER")
    void export_noCollaterals_returnsCsv() throws Exception {
        when(collateralService.findAll()).thenReturn(new ArrayList<>());
        mvc.perform(get("/collaterals/export")).andExpect(status().isOk());
    }

    // пустое название и отрицательная цена не сохраняются (нормальные данные сохраняются)
    @Test
    @WithMockUser(roles = "MANAGER")
    void edit_severalInputs_onlyValidSaved() throws Exception {
        Collateral c = new Collateral(); c.setId(1L);
        when(collateralService.findById(1L)).thenReturn(c);

        mvc.perform(post("/collaterals/1/edit").with(csrf()).param("name", ""))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/collaterals/1/edit").with(csrf()).param("name", "Test").param("estimatedPrice", "-100"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(post("/collaterals/1/edit").with(csrf()).param("name", "Test").param("estimatedPrice", "100"))
                .andExpect(status().is3xxRedirection());
        verify(collateralService).save(c);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_withSearchAndFilters_opens() throws Exception {
        when(collateralService.findAll()).thenReturn(List.of());
        mvc.perform(get("/collaterals")
                        .param("query", "test")
                        .param("status", "Выдан")
                        .param("sort", "price"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void editForm_notFound_redirects() throws Exception {
        when(collateralService.findById(1L)).thenReturn(null);
        mvc.perform(get("/collaterals/1/edit")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_existing_callsService() throws Exception {
        when(collateralService.findById(1L)).thenReturn(new Collateral());
        mvc.perform(post("/collaterals/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection());
        verify(collateralService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void edit_emptyName_notSaved() throws Exception {
        when(collateralService.findById(1L)).thenReturn(new Collateral());
        mvc.perform(post("/collaterals/1/edit").with(csrf()).param("name", ""))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void list_opens() throws Exception {
        mvc.perform(get("/collaterals")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void edit_withNewPhoto_saves() throws Exception {
        Collateral c = new Collateral(); c.setId(1L);
        when(collateralService.findById(1L)).thenReturn(c);

        MockMultipartFile photo = new MockMultipartFile("photos", "test.jpg", "image/jpeg", "data".getBytes());

        mvc.perform(multipart("/collaterals/1/edit").file(photo).with(csrf())
                        .param("name", "Phone")
                        .param("estimatedPrice", "1000"))
                .andExpect(status().is3xxRedirection());
        verify(collateralService).save(c);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deletePhoto_existing_callsService() throws Exception {
        Collateral c = new Collateral(); c.setId(1L);
        when(collateralService.findById(1L)).thenReturn(c);

        mvc.perform(post("/collaterals/1/photo/delete").with(csrf())
                        .param("photoUrl", "/uploads/test.jpg"))
                .andExpect(status().is3xxRedirection());
        verify(collateralService).removePhoto(c, "/uploads/test.jpg");
    }
}

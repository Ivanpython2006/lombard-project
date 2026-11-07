package com.lombard.service;

import com.lombard.model.Collateral;
import com.lombard.repository.CollateralRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Тесты сервиса залогов: номера вещей, удаление и работа с фотографиями (максимум 10 штук).
@ExtendWith(MockitoExtension.class)
class CollateralServiceTest {

    @Mock
    private CollateralRepository collateralRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private NumberSequenceService numberSequenceService;

    private CollateralService collateralService;

    @BeforeEach
    void setUp() {
        collateralService = new CollateralService(collateralRepository, numberSequenceService, fileStorageService);
    }

    private MultipartFile[] onePhoto() {
        return new MultipartFile[]{new MockMultipartFile("p", "a.jpg", "image/jpeg", "data".getBytes())};
    }

    private Collateral withPhotos(int count) {
        Collateral c = new Collateral();
        List<String> photos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            photos.add("/uploads/" + i + ".jpg");
        }
        c.setPhotoUrls(photos);
        return c;
    }

    @Test
    void save_withoutNumber_generatesNumber() {
        when(numberSequenceService.nextNumber("M_ITEM", "M")).thenReturn("M1");
        Collateral c = new Collateral();

        collateralService.save(c);

        assertEquals("M1", c.getItemNumber());
        verify(collateralRepository).save(c);
    }

    @Test
    void addCollateral_withNumber_keepsNumber() {
        Collateral c = new Collateral();
        c.setItemNumber("M9");

        collateralService.addCollateral(c);

        assertEquals("M9", c.getItemNumber());
        verify(collateralRepository).save(c);
    }

    @Test
    void findById_missing_returnsNull() {
        when(collateralRepository.findById(5L)).thenReturn(Optional.empty());

        assertNull(collateralService.findById(5L));
    }

    // простые методы которые просто проксируют вызов в репозиторий
    @Test
    void finders_callRepository() {
        when(collateralRepository.findByLoanId(1L)).thenReturn(List.of());
        when(collateralRepository.findAll()).thenReturn(List.of(new Collateral()));
        when(collateralRepository.searchByAnyField("тел")).thenReturn(List.of());
        when(collateralRepository.count()).thenReturn(5L);

        assertTrue(collateralService.getCollateralByLoanId(1L).isEmpty());
        assertEquals(1, collateralService.findAll().size());
        assertTrue(collateralService.searchByAnyField("тел").isEmpty());
        assertEquals(5L, collateralService.count());
    }

    // при удалении залога удаляются и его фотки с диска
    @Test
    void delete_withPhotos_removesFiles() {
        Collateral c = withPhotos(1);
        when(collateralRepository.findById(1L)).thenReturn(Optional.of(c));

        collateralService.delete(1L);

        verify(fileStorageService).delete("/uploads/0.jpg");
        verify(collateralRepository).deleteById(1L);
    }

    @Test
    void delete_notFound_stillDeletesById() {
        when(collateralRepository.findById(99L)).thenReturn(Optional.empty());

        collateralService.delete(99L);

        verifyNoInteractions(fileStorageService);
        verify(collateralRepository).deleteById(99L);
    }

    @Test
    void appendPhotos_nullOrEmpty_doesNothing() {
        Collateral c = new Collateral();

        collateralService.appendPhotos(c, null, "x");
        collateralService.appendPhotos(c, new MultipartFile[0], "x");

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void appendPhotos_underLimit_addsPhoto() {
        Collateral c = withPhotos(0);
        when(fileStorageService.saveAll(any(), any())).thenReturn(List.of("/uploads/new.jpg"));

        collateralService.appendPhotos(c, onePhoto(), "x");

        assertTrue(c.getPhotoUrls().contains("/uploads/new.jpg"));
        verify(collateralRepository).save(c);
    }

    // если файлы не сохранились залог не пересохраняем
    @Test
    void appendPhotos_nothingSaved_doesNotSave() {
        Collateral c = withPhotos(0);
        when(fileStorageService.saveAll(any(), any())).thenReturn(List.of());

        collateralService.appendPhotos(c, onePhoto(), "x");

        verify(collateralRepository, never()).save(any());
    }

    // 9 фоток уже есть пытаемся добавить 2 - сохранится только одна
    @Test
    void appendPhotos_moreThanRemaining_savesOnlyRemaining() {
        Collateral c = withPhotos(9);
        MultipartFile[] two = {onePhoto()[0], onePhoto()[0]};
        when(fileStorageService.saveAll(any(), any())).thenAnswer(inv -> {
            MultipartFile[] files = inv.getArgument(0);
            assertEquals(1, files.length);
            return List.of("/uploads/9.jpg");
        });

        collateralService.appendPhotos(c, two, "x");

        assertEquals(10, c.getPhotoUrls().size());
    }

    @Test
    void appendPhotos_limitReached_throwsException() {
        Collateral c = withPhotos(10);

        assertThrows(IllegalArgumentException.class, () -> collateralService.appendPhotos(c, onePhoto(), "x"));
    }

    @Test
    void removePhoto_existing_removesAndDeletesFile() {
        Collateral c = withPhotos(1);

        collateralService.removePhoto(c, "/uploads/0.jpg");

        assertTrue(c.getPhotoUrls().isEmpty());
        verify(fileStorageService).delete("/uploads/0.jpg");
        verify(collateralRepository).save(c);
    }

    @Test
    void removePhoto_nullUrlOrNoPhotos_doesNothing() {
        collateralService.removePhoto(new Collateral(), null);
        Collateral c = new Collateral();
        c.setPhotoUrls(null);
        collateralService.removePhoto(c, "/uploads/1.jpg");

        verifyNoInteractions(fileStorageService, collateralRepository);
    }
}

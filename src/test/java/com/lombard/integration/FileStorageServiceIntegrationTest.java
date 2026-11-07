package com.lombard.integration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

// Интеграционные тесты сохранения фотографий на диск (папка из настроек тестов)
class FileStorageServiceIntegrationTest extends IntegrationTestBase {

    @Test
    void save_emptyFile_returnsNull() {
        MockMultipartFile f = new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
        assertNull(fileStorageService.save(f, "test"));
    }

    @Test
    void save_null_returnsNull() {
        assertNull(fileStorageService.save(null, "test"));
    }

    @Test
    void save_notImage_throwsException() {
        MockMultipartFile f = new MockMultipartFile("file", "a.gif", "image/gif", new byte[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.save(f, "test"));
    }

    // файл называется .jpg но внутри не картинка
    @Test
    void save_brokenImage_throwsException() {
        MockMultipartFile f = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.save(f, "test"));
    }

    @Test
    void save_validJpeg_returnsUploadsUrl() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "a.jpg", "image/jpeg", realJpegBytes());
        String url = fileStorageService.save(f, "test");
        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/"));

        fileStorageService.delete(url);
    }

    @Test
    void save_tooLarge_throwsException() {
        byte[] big = new byte[6 * 1024 * 1024];
        MockMultipartFile f = new MockMultipartFile("file", "a.jpg", "image/jpeg", big);
        assertThrows(IllegalArgumentException.class, () -> fileStorageService.save(f, "test"));
    }

    @Test
    void saveAll_nullOrEmpty_returnsEmptyList() {
        assertTrue(fileStorageService.saveAll(new MockMultipartFile[0], "p").isEmpty());
        assertTrue(fileStorageService.saveAll(null, "p").isEmpty());
    }

    // файлы вне папки uploads удалять нельзя
    @Test
    void delete_pathOutsideUploads_ignored() {
        fileStorageService.delete("/other/path.jpg");
        fileStorageService.delete(null);
    }

    private byte[] realJpegBytes() throws Exception {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", out);
        return out.toByteArray();
    }
}

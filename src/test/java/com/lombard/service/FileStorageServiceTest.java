package com.lombard.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//Тесты проверки загружаемых фотографий и их удаления (удаление проверяю во временной папке)
class FileStorageServiceTest {

    private FileStorageService createService() {
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", "./uploads-test/");
        return service;
    }

    @Test
    void save_emptyFile_returnsNull() {
        FileStorageService service = createService();
        MockMultipartFile f = new MockMultipartFile("f", "test.jpg", "image/jpeg", new byte[0]);
        assertThat(service.save(f, "prefix")).isNull();
    }

    // грузить можно только картинки
    @Test
    void save_notImage_throwsException() {
        FileStorageService service = createService();
        MockMultipartFile f = new MockMultipartFile("f", "test.txt", "text/plain", "hello".getBytes());
        assertThatThrownBy(() -> service.save(f, "prefix"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // файл 6 МБ больше лимита
    @Test
    void save_fileTooBig_throwsException() {
        FileStorageService service = createService();
        byte[] big = new byte[6 * 1024 * 1024];
        MockMultipartFile f = new MockMultipartFile("f", "test.jpg", "image/jpeg", big);
        assertThatThrownBy(() -> service.save(f, "prefix"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // фото из папки загрузок удаляется
    @Test
    void delete_fileInUploadDir_removesFile(@TempDir Path tmp) throws IOException {
        Path photo = Files.writeString(tmp.resolve("item_1_0_abc.jpg"), "photo");
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", tmp.toString());

        service.delete("/uploads/item_1_0_abc.jpg");

        assertThat(photo).doesNotExist();
    }

    // через ".." нельзя выйти из папки загрузок и удалить чужой файл
    @Test
    void delete_pathOutsideUploadDir_keepsFile(@TempDir Path tmp) throws IOException {
        Path uploads = Files.createDirectory(tmp.resolve("uploads"));
        Path secret = Files.writeString(tmp.resolve("secret.txt"), "data");
        FileStorageService service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadDir", uploads.toString());

        service.delete("/uploads/../secret.txt");
        service.delete("/uploads/");

        assertThat(secret).exists();
        assertThat(uploads).exists();
    }
}
package com.lombard.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

//Тесты проверки загружаемых фотографий (без реального сохранения на диск)
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
}
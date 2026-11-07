package com.lombard.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String save(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) return null;

        try {
            Path dir = Paths.get(uploadDir);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String contentType = file.getContentType();
            if (contentType == null
                    || (!contentType.equals("image/jpeg")
                    && !contentType.equals("image/jpg")
                    && !contentType.equals("image/png"))) {
                throw new IllegalArgumentException("Только JPG и PNG. Другие форматы не поддерживаются.");
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Файл слишком большой (макс 5 МБ)");
            }

            byte[] bytes = file.getBytes();
            if (!isRealImage(bytes)) {
                throw new IllegalArgumentException("Файл не является изображением");
            }

            String original = file.getOriginalFilename();
            String ext = ".jpg";
            if (original != null && original.contains(".")) {
                String candidate = original.substring(original.lastIndexOf(".")).toLowerCase();
                if (candidate.matches("\\.(jpg|jpeg|png)")) {
                    ext = candidate;
                }
            }

            String filename = prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(filename);
            Files.copy(new ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл: " + e.getMessage(), e);
        }
    }

    public List<String> saveAll(MultipartFile[] files, String prefix) {
        List<String> urls = new ArrayList<>();
        if (files == null || files.length == 0) return urls;
        for (int i = 0; i < files.length; i++) {
            MultipartFile f = files[i];
            if (f == null || f.isEmpty()) continue;
            String url = save(f, prefix + "_" + i);
            if (url != null) urls.add(url);
        }
        return urls;
    }

    public void delete(String photoUrl) {
        if (photoUrl == null || !photoUrl.startsWith("/uploads/")) return;
        try {
            String filename = photoUrl.substring("/uploads/".length());
            Path target = Paths.get(uploadDir).resolve(filename);
            Files.deleteIfExists(target);
        } catch (IOException e) {
            System.err.println("Не удалось удалить файл " + photoUrl + ": " + e.getMessage());
        }
    }

    private boolean isRealImage(byte[] bytes) {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(in);
            return img != null;
        } catch (IOException e) {
            return false;
        }
    }
}
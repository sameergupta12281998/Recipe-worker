package com.example.recipe_worker.service.impl;

import com.example.recipe_worker.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    @Value("${recipe-worker.storage.base-dir:./data/storage}")
    private String baseDir;

    @PostConstruct
    public void init() {
        try {
            Path base = Paths.get(baseDir);
            if (!Files.exists(base)) {
                Files.createDirectories(base);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create base storage directory: " + baseDir, e);
        }
    }

    @Override
    public String store(byte[] data, String filename) {
        try {
            String safeName = UUID.randomUUID().toString() + "-" + sanitizeFilename(filename);
            Path path = Paths.get(baseDir).resolve(safeName);
            Files.write(path, data);
            return path.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
    }

    /**
     * Prevent directory traversal and invalid filenames
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) return "unnamed";
        // Remove any path separators or illegal characters
        return filename.replaceAll("[\\\\/]+", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

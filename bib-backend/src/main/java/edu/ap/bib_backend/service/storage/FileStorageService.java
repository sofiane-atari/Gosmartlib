package edu.ap.bib_backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf",
            "doc",
            "docx",
            "ppt",
            "pptx",
            "xls",
            "xlsx"
    );

    private final Path storageDirectory;

    public FileStorageService(
            @Value("${app.didactic-material.attachments-directory:uploads/didactic-material}") String storageDirectory
    ) {
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        createStorageDirectoryIfNeeded();
    }

    public String storeFile(MultipartFile file) {
        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + "." + extension;

        Path targetPath = storageDirectory.resolve(storedFileName).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException exception) {
            throw new IllegalStateException("Bestand kon niet worden opgeslagen.", exception);
        }
    }

    public Resource loadAsResource(String storedFileName) {
        Path filePath = storageDirectory.resolve(storedFileName).normalize();
        Resource resource = new PathResource(filePath);

        if (!resource.exists() || !resource.isReadable()) {
            throw new IllegalStateException("Bestand kon niet worden gevonden.");
        }

        return resource;
    }

    public void deleteFile(String storedFileName) {
        Path filePath = storageDirectory.resolve(storedFileName).normalize();

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Bestand kon niet worden verwijderd.", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Er werd geen bestand geselecteerd.");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = getFileExtension(originalFileName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Ongeldig bestandstype. Alleen PDF, Word, Excel en PowerPoint bestanden zijn toegelaten."
            );
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.contains(".")) {
            throw new IllegalArgumentException("Bestandsnaam is ongeldig.");
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);

        if (extension.isBlank()) {
            throw new IllegalArgumentException("Bestandsnaam is ongeldig.");
        }

        return extension;
    }

    private void createStorageDirectoryIfNeeded() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Storage map voor didactisch materiaal kon niet worden aangemaakt.", exception);
        }
    }
}
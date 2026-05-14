package edu.ap.bib_backend.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;


import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    void Given_ValidPdfFile_When_StoreFile_Then_ReturnsStoredFileName() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());

        // Act
        String result = fileStorageService.storeFile(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith(".pdf"));
    }

    @Test
    void Given_ValidWordFile_When_StoreFile_Then_ReturnsStoredFileName() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "test content".getBytes());

        // Act
        String result = fileStorageService.storeFile(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith(".docx"));
    }

    @Test
    void Given_ValidExcelFile_When_StoreFile_Then_ReturnsStoredFileName() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "test content".getBytes());

        // Act
        String result = fileStorageService.storeFile(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith(".xlsx"));
    }

    @Test
    void Given_ValidPowerPointFile_When_StoreFile_Then_ReturnsStoredFileName() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "test content".getBytes());

        // Act
        String result = fileStorageService.storeFile(file);

        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith(".pptx"));
    }

    @Test
    void Given_NullFile_When_StoreFile_Then_ThrowsIllegalArgumentException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", (byte[]) null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fileStorageService.storeFile(file));
        assertEquals("Er werd geen bestand geselecteerd.", exception.getMessage());
    }

    @Test
    void Given_EmptyFile_When_StoreFile_Then_ThrowsIllegalArgumentException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[0]);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fileStorageService.storeFile(file));
        assertEquals("Er werd geen bestand geselecteerd.", exception.getMessage());
    }

    @Test
    void Given_InvalidExtension_When_StoreFile_Then_ThrowsIllegalArgumentException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test content".getBytes());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fileStorageService.storeFile(file));
        assertTrue(exception.getMessage().contains("Ongeldig bestandstype"));
    }

    @Test
    void Given_NullFileName_When_StoreFile_Then_ThrowsIllegalArgumentException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "application/pdf", "test content".getBytes());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fileStorageService.storeFile(file));
        assertEquals("Bestandsnaam is ongeldig.", exception.getMessage());
    }

    @Test
    void Given_BlankFileName_When_StoreFile_Then_ThrowsIllegalArgumentException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "   ", "application/pdf", "test content".getBytes());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fileStorageService.storeFile(file));
        assertEquals("Bestandsnaam is ongeldig.", exception.getMessage());
    }

    @Test
    void Given_FileNameWithoutExtension_When_StoreFile_Then_ThrowsIllegalArgumentException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "noextension", "application/pdf", "test content".getBytes());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            fileStorageService.storeFile(file));
        assertEquals("Bestandsnaam is ongeldig.", exception.getMessage());
    }

    @Test
    void Given_StoredFile_When_LoadAsResource_Then_ReturnsResource() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        String storedFileName = fileStorageService.storeFile(file);

        // Act
        Resource resource = fileStorageService.loadAsResource(storedFileName);

        // Assert
        assertNotNull(resource);
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());
    }

    @Test
    void Given_NonExistentFile_When_LoadAsResource_Then_ThrowsIllegalStateException() {
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            fileStorageService.loadAsResource("nonexistent.pdf"));
        assertEquals("Bestand kon niet worden gevonden.", exception.getMessage());
    }

    @Test
    void Given_StoredFile_When_DeleteFile_Then_FileIsDeleted() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "test content".getBytes());
        String storedFileName = fileStorageService.storeFile(file);

        // Act
        fileStorageService.deleteFile(storedFileName);

        // Assert
        assertThrows(IllegalStateException.class, () ->
            fileStorageService.loadAsResource(storedFileName));
    }

    @Test
    void Given_NonExistentFile_When_DeleteFile_Then_NoException() {
        // Act & Assert - should not throw
        assertDoesNotThrow(() -> fileStorageService.deleteFile("nonexistent.pdf"));
    }

    @Test
    void Given_UpperCaseExtension_When_StoreFile_Then_StoredWithLowerCase() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.PDF", "application/pdf", "test content".getBytes());

        // Act
        String result = fileStorageService.storeFile(file);

        // Assert
        assertTrue(result.endsWith(".pdf"));
    }
}
package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.service.BookBulkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.File;

class BookBulkControllerTest {

    private final BookBulkService bbs = Mockito.mock(BookBulkService.class);
    private final BookBulkController controller = new BookBulkController(bbs);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    @Test
    @DisplayName("POST /api/books/upload moet 400 geven als bestand geen xlsx is")
    void uploadBooks_ShouldReturnBadRequest_WhenFileIsNotXlsx() throws Exception {
        mockMvc.perform(
                multipart("/books/upload")
                        .file("file", "fake content".getBytes())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("file", "books.csv"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/books/upload moet 200 geven als xlsx bestand correct is")
    void uploadBooks_ShouldReturnOk_WhenFileIsXlsx() throws Exception {
        doNothing().when(bbs).importBooks(any(MultipartFile.class));

        mockMvc.perform(
                multipart("/books/upload")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file",
                                "books.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "fake excel content".getBytes())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Books uploaded successfully"));
    }

    @Test
    @DisplayName("GET /api/books/template moet template downloaden")
    void downloadTemplate_ShouldReturnResource() throws Exception {
        mockMvc.perform(get("/books/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=books_template.xlsx"));
    }

    @Test
    @DisplayName("POST /books/upload/temp moet temp file id teruggeven")
    void uploadTempFile_ShouldReturnTempId() throws Exception {
        when(bbs.storeTempFile(any(MultipartFile.class))).thenReturn("temp-123.xlsx");

        mockMvc.perform(
                multipart("/books/upload/temp")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "file",
                                "books.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "fake excel content".getBytes())))
                .andExpect(status().isOk())
                .andExpect(content().string("temp-123.xlsx"));
    }

    @Test
    @DisplayName("GET /api/books/upload/validate-stream/{tempFileId} moet SSE stream starten")
    void validateStream_ShouldReturnSseEmitter() throws Exception {
        SseEmitter emitter = new SseEmitter();
        File dummyFile = new File("dummy.xlsx");

        when(bbs.getTempFile("temp-123.xlsx")).thenReturn(dummyFile);
        when(bbs.validateBooksStream(dummyFile)).thenReturn(emitter);

        mockMvc.perform(get("/books/upload/validate-stream/temp-123.xlsx"))
                .andExpect(status().isOk());
    }
}
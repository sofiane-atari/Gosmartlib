package edu.ap.bib_backend.controller;

import edu.ap.bib_backend.service.book.BookBulkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookBulkControllerTest {

        private final BookBulkService bbs = Mockito.mock(BookBulkService.class);
        private final BookBulkController controller = new BookBulkController(bbs);
        private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        @Test
        @DisplayName("POST /books/upload moet 400 geven als bestand geen xlsm is")
        void uploadBooks_ShouldReturnBadRequest_WhenFileIsNotXlsm() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "books.csv",
                                "text/csv",
                                "fake content".getBytes());

                mockMvc.perform(
                                multipart("/books/upload").file(file))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Only excel files are supported"));
        }

//        @Test
//        @DisplayName("POST /books/upload moet 200 geven als xlsm bestand correct is")
//        void uploadBooks_ShouldReturnOk_WhenFileIsXlsm() throws Exception {
//                 doNothing().when(bbs).importBooks(any(MultipartFile.class));
//
//                MockMultipartFile file = new MockMultipartFile(
//                                "file",
//                                "books.xlsm",
//                                "application/vnd.ms-excel.sheet.macroEnabled.12",
//                                "fake excel content".getBytes());
//
//                mockMvc.perform(
//                                multipart("/books/upload").file(file))
//                                .andExpect(status().isOk())
//                                .andExpect(jsonPath("$.message").value("Books uploaded successfully"));
//        }

        @Test
        @DisplayName("GET /books/template moet template downloaden")
        void downloadTemplate_ShouldReturnResource() throws Exception {
                mockMvc.perform(get("/books/template"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Disposition",
                                                "attachment; filename=books_template.xlsm"));
        }

        @Test
        @DisplayName("POST /books/upload/temp moet temp file id teruggeven")
        void uploadTempFile_ShouldReturnTempId() throws Exception {
                when(bbs.storeTempFile(any(MultipartFile.class))).thenReturn("temp-123.xlsm");

                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "books.xlsm",
                                "application/vnd.ms-excel.sheet.macroEnabled.12",
                                "fake excel content".getBytes());

                mockMvc.perform(
                                multipart("/books/upload/temp").file(file))
                                .andExpect(status().isOk())
                                .andExpect(content().string("temp-123.xlsm"));
        }

        @Test
        @DisplayName("GET /books/upload/validate-stream/{tempFileId} moet SSE stream starten")
        void validateStream_ShouldReturnSseEmitter() throws Exception {
                SseEmitter emitter = new SseEmitter();
                File dummyFile = new File("dummy.xlsm");

                when(bbs.getTempFile("temp-123.xlsm")).thenReturn(dummyFile);
                when(bbs.validateBooksStream(dummyFile)).thenReturn(emitter);

                mockMvc.perform(get("/books/upload/validate-stream/temp-123.xlsm"))
                                .andExpect(status().isOk());
        }
}
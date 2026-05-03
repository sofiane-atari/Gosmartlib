package edu.ap.bib_backend.service.lessontip;

import edu.ap.bib_backend.exception.ResourceNotFoundException;
import edu.ap.bib_backend.jpa.entity.Book;
import edu.ap.bib_backend.jpa.entity.lessontips.LessonTip;
import edu.ap.bib_backend.jpa.repository.BookRepository;
import edu.ap.bib_backend.service.lessontips.LessonTipBookLinkService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class LessonTipBookLinkServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private LessonTipBookLinkService didacticMaterialBookLinkService;

    @Test
    void givenNullLinkedBookIds_whenReplaceBookLinks_thenClearLinksWithoutLoadingBooks() {
        // Arrange
        LessonTip material = new LessonTip();

        // Act
        didacticMaterialBookLinkService.replaceBookLinks(material, null);

        // Assert
        assertThat(material.getBookLinks()).isEmpty();
        then(bookRepository).shouldHaveNoInteractions();
    }


    @Test
    void givenMissingBookId_whenReplaceBookLinks_thenThrowResourceNotFoundException() {
        // Arrange
        LessonTip material = new LessonTip();
        Book firstBook = createBook(10L, "Boek 1");

        given(bookRepository.findAllById(List.of(10L, 20L)))
                .willReturn(List.of(firstBook));

        // Act
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> didacticMaterialBookLinkService.replaceBookLinks(material, List.of(10L, 20L))
        );

        // Assert
        assertThat(exception.getFieldName()).isEqualTo("linkedBookIds");
        assertThat(exception.getMessage()).contains("Boek niet gevonden met id: 20");

        then(bookRepository).should().findAllById(List.of(10L, 20L));
    }

    @Test
    void givenEmptyLinkedBookIds_whenReplaceBookLinks_thenClearExistingLinks() {
        // Arrange
        LessonTip material = new LessonTip();
        material.replaceBookLinks(List.of(createBook(10L, "Boek 1"), createBook(20L, "Boek 2")));

        // Act
        didacticMaterialBookLinkService.replaceBookLinks(material, List.of());

        // Assert
        assertThat(material.getBookLinks()).isEmpty();
        then(bookRepository).shouldHaveNoInteractions();
    }

    private Book createBook(Long id, String title) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("Auteur");
        return book;
    }
}
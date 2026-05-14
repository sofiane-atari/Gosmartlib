package edu.ap.bib_backend.service.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultMessageStylesTest {

    private final DefaultMessageStyles styles = new DefaultMessageStyles();

    @Test
    void getStyleBlock_containsBtnPrimary() {
        String css = styles.getStyleBlock();
        assertTrue(css.contains(".msg-btn-primary"));
    }

    @Test
    void getStyleBlock_containsBookLink() {
        String css = styles.getStyleBlock();
        assertTrue(css.contains(".msg-book-link"));
    }

    @Test
    void getStyleBlock_containsBookCard() {
        String css = styles.getStyleBlock();
        assertTrue(css.contains(".msg-book-card"));
    }

    @Test
    void getStyleBlock_containsCoverLink() {
        String css = styles.getStyleBlock();
        assertTrue(css.contains(".msg-cover-link"));
    }

    @Test
    void getStyleBlock_containsHoverTransitions() {
        String css = styles.getStyleBlock();
        assertTrue(css.contains(":hover"));
    }

    @Test
    void getStyleBlock_containsStyleTag() {
        String css = styles.getStyleBlock();
        assertTrue(css.contains("<style>"));
        assertTrue(css.contains("</style>"));
    }

    @Test
    void getStyleBlock_isNotEmpty() {
        String css = styles.getStyleBlock();
        assertNotNull(css);
        assertFalse(css.isBlank());
    }

    @Test
    void constants_matchStyleBlockClassNames() {
        assertTrue(styles.getStyleBlock().contains(MessageStyles.BTN_PRIMARY));
        assertTrue(styles.getStyleBlock().contains(MessageStyles.BOOK_LINK));
        assertTrue(styles.getStyleBlock().contains(MessageStyles.BOOK_CARD));
        assertTrue(styles.getStyleBlock().contains(MessageStyles.COVER_LINK));
    }
}

package edu.ap.bib_backend.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import edu.ap.bib_backend.jpa.entity.BookCopy;
import edu.ap.bib_backend.jpa.enums.BookCondition;

class BookCopyTest {

    @Test
    void constructor_setsDefaultConditionToGoodCondition() {
        BookCopy copy = new BookCopy();

        assertThat(copy.getCondition()).isEqualTo(BookCondition.GOOD_CONDITION);
    }
}

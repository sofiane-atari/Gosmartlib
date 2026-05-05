import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { DidacticMaterialBookSelectionPanelComponent } from '../../../didactic/didactic-material-book-selection-panel/didactic-material-book-selection-panel.component';
import { BookCandidate } from '../../../../model/book/book-candidate.model';
import { Book } from '../../../../model/book/book.model';

@Component({
  selector: 'app-book-selection-panel',
  standalone: true,
  imports: [CommonModule, DidacticMaterialBookSelectionPanelComponent],
  templateUrl: './book-selection-panel.component.html',
  styleUrl: './book-selection-panel.component.css',
})
export class BookSelectionPanelComponent {
  public readonly hasSelectedUser = input.required<boolean>();
  public readonly selectedBooksCount = input.required<number>();
  public readonly searchQuery = input.required<string>();
  public readonly searchIsbn = input.required<string>();
  public readonly isLoadingBooks = input.required<boolean>();
  public readonly hasBookSearchInput = input.required<boolean>();
  public readonly filteredBooks = input.required<Book[]>();
  public readonly selectedBookIds = input.required<number[]>();
  public readonly selectedBookCounts = input.required<Record<number, number>>();

  public readonly searchQueryChanged = output<Event>();
  public readonly isbnSearchChanged = output<Event>();
  public readonly isbnKeydown = output<KeyboardEvent>();
  public readonly addBookRequested = output<Book>();

  public readonly bookCandidates = computed(() => {
    return this.filteredBooks().map((book) => ({
      id: book.id,
      title: book.title,
      author: book.author,
      isbn13: book.isbn ?? '',
      releaseYear: book.releaseYear ?? null,
      imageUrl: book.imageUrl,
      availableCopies: book.availableCopies,
      selectedCount: book.id !== undefined ? (this.selectedBookCounts()[book.id] ?? 0) : 0,
    }));
  });

  public handleAddBookRequested(book: BookCandidate): void {
    const matchedBook = this.filteredBooks().find((entry) => {
      if (book.id !== undefined) {
        return entry.id === book.id;
      }

      return entry.isbn === book.isbn13;
    });

    if (!matchedBook) {
      return;
    }

    this.addBookRequested.emit(matchedBook);
  }
}

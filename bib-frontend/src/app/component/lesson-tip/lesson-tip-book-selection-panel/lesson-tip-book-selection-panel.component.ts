import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { BookCandidate } from '../../../model/book/book-candidate.model';

@Component({
  selector: 'app-lesson-tip-book-selection-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lesson-tip-book-selection-panel.component.html',
styleUrl: './lesson-tip-book-selection-panel.component.css',
})
export class LessonTipBookSelectionPanelComponent {
  public readonly panelEyebrow = input<string>('Gekoppelde boeken');
  public readonly panelTitle = input<string>('Zoek en selecteer boeken');
  public readonly hasSelectedUser = input<boolean>(true);
  public readonly allowMultipleSelection = input<boolean>(false);
  public readonly searchQuery = input.required<string>();
  public readonly searchIsbn = input.required<string>();
  public readonly isLoadingBooks = input.required<boolean>();
  public readonly hasBookSearchInput = input.required<boolean>();
  public readonly filteredBooks = input.required<BookCandidate[]>();
  public readonly selectedBooksCount = input.required<number>();
  public readonly selectedBookIds = input.required<number[]>();

  public readonly searchQueryChanged = output<Event>();
  public readonly isbnSearchChanged = output<Event>();
  public readonly isbnKeydown = output<KeyboardEvent>();
  public readonly addBookRequested = output<BookCandidate>();

  protected isBookAlreadySelected(bookId: number): boolean {
    return this.selectedBookIds().includes(bookId);
  }

  protected isSelectDisabled(book: BookCandidate): boolean {
    if (book.id === undefined) {
      return true;
    }

    if (!this.allowMultipleSelection()) {
      return this.isBookAlreadySelected(book.id);
    }

    const availableCopies = book.availableCopies ?? 0;
    const selectedCount = book.selectedCount ?? 0;
    return availableCopies === 0 || selectedCount >= availableCopies;
  }

  protected getActionLabel(book: BookCandidate): string {
    if (book.id === undefined) {
      return 'Niet beschikbaar';
    }

    if (!this.allowMultipleSelection()) {
      return this.isBookAlreadySelected(book.id)
        ? 'Geselecteerd'
        : 'Selecteren';
    }

    const availableCopies = book.availableCopies ?? 0;
    const selectedCount = book.selectedCount ?? 0;

    if (availableCopies === 0) {
      return 'Niet beschikbaar';
    }

    if (selectedCount >= availableCopies) {
      return 'Volzet';
    }

    return 'Selecteren';
  }

  protected getAvailableCopiesLabel(book: BookCandidate): string {
    if (book.availableCopies === undefined) {
      return '';
    }

    if (book.availableCopies === 0) {
      return 'Geen exemplaren beschikbaar';
    }

    if (book.availableCopies === 1) {
      return '1 exemplaar beschikbaar';
    }

    return `${book.availableCopies} exemplaren beschikbaar`;
  }

  protected getSelectedCopiesLabel(book: BookCandidate): string {
    if (!this.allowMultipleSelection()) {
      return '';
    }

    if (!book.selectedCount) {
      return '';
    }

    if (book.selectedCount === 1) {
      return '1 geselecteerd';
    }

    return `${book.selectedCount} geselecteerd`;
  }
}

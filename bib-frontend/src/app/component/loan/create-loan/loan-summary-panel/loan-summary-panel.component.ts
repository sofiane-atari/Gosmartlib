import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Book, BookCopyLocation, BookCondition } from '../../../../model/book/book.model';
import { SelectedBookItemComponent } from '../selected-book-item/selected-book-item.component';

@Component({
  selector: 'app-loan-summary-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SelectedBookItemComponent],
  templateUrl: './loan-summary-panel.component.html',
  styleUrl: './loan-summary-panel.component.css',
})
export class LoanSummaryPanelComponent {
  public readonly loanForm = input.required<FormGroup>();
  public readonly minReturnDate = input.required<string>();
  public readonly selectedBooks = input.required<Book[]>();
  public readonly selectedBookCounts = input.required<Record<number, number>>();
  public readonly selectedCopyIdsByBook = input.required<Record<number, number[]>>();
  public readonly hasSelectedUser = input.required<boolean>();
  public readonly isSubmitting = input.required<boolean>();
  public readonly defaultLoanDays = input<number>();
  public readonly isCustomDate = input<boolean>(false);
  public readonly isCustomDateChange = output<boolean>();

  public readonly createLoanRequested = output<void>();
  public readonly removeBookRequested = output<number | undefined>();
  public readonly copySelectionChanged = output<{ bookId: number; index: number; copyId: number }>();
  public readonly customDateRequested = output<void>();
  public readonly useDefaultRequested = output<void>();

  public readonly daysUntilReturn = computed(() => {
    const returnDateStr = this.loanForm().value.returnDate;
    if (!returnDateStr) return null;

    const returnDate = new Date(returnDateStr);
    const today = new Date();
    const diffTime = returnDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
  });

  public readonly groupedSelectedBooks = computed(() => {
    const counts = this.selectedBookCounts();
    const uniqueBooks = new Map<number, Book>();

    this.selectedBooks().forEach((book) => {
      if (book.id === undefined) {
        return;
      }

      if (!uniqueBooks.has(book.id)) {
        uniqueBooks.set(book.id, book);
      }
    });

    return Array.from(uniqueBooks.values()).map((book) => ({
      book,
      count: counts[book.id ?? 0] ?? 1,
    }));
  });

  public trackSelectedBook(index: number, book: Book): number | string {
    return book.id ?? `book-${book.isbn ?? index}`;
  }

  public getSelectedCount(book: Book): number {
    if (book.id === undefined) {
      return 1;
    }

    return this.selectedBookCounts()[book.id] ?? 1;
  }

  public getCopySlots(count: number): number[] {
    return Array.from({ length: count }, (_, index) => index);
  }

  public getAvailableCopies(book: Book): BookCopyLocation[] {
    return (book.copyLocations ?? [])
      .flatMap((location) => location.copies)
      .filter((copy) => copy.availability === 'AVAILABLE' && copy.condition !== 'LOST');
  }

  public getSelectedCopyId(book: Book, index: number): number | null {
    if (book.id === undefined) {
      return null;
    }

    return this.selectedCopyIdsByBook()[book.id]?.[index] ?? null;
  }

  public isCopySelectedInOtherSlot(book: Book, copyId: number, index: number): boolean {
    if (book.id === undefined) {
      return false;
    }

    return (this.selectedCopyIdsByBook()[book.id] ?? []).some(
      (selectedCopyId, selectedIndex) => selectedCopyId === copyId && selectedIndex !== index,
    );
  }

  public onCopySelectionChange(book: Book, index: number, event: Event): void {
    if (book.id === undefined) {
      return;
    }

    const copyId = Number((event.target as HTMLSelectElement).value);
    if (!Number.isFinite(copyId)) {
      return;
    }

    this.copySelectionChanged.emit({ bookId: book.id, index, copyId });
  }

  public getCopyLabel(copy: BookCopyLocation, index: number): string {
    return `Exemplaar ${index + 1} - ${this.getConditionLabel(copy.condition)}`;
  }

  public getConditionLabel(condition?: BookCondition): string {
    switch (condition) {
      case 'DAMAGED':
        return 'Beschadigd';
      case 'LOST':
        return 'Verloren';
      case 'GOOD_CONDITION':
      default:
        return 'Goede staat';
    }
  }

}


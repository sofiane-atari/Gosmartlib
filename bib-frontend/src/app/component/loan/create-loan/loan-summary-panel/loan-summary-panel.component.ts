import { CommonModule } from '@angular/common';
import { Component, computed, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Book } from '../../../../model/book/book.model';
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
  public readonly hasSelectedUser = input.required<boolean>();
  public readonly isSubmitting = input.required<boolean>();

  public readonly createLoanRequested = output<void>();
  public readonly removeBookRequested = output<number | undefined>();

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
}

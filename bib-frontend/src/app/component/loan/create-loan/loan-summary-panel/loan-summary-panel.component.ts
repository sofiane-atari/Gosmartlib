import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
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
  public readonly hasSelectedUser = input.required<boolean>();
  public readonly isSubmitting = input.required<boolean>();

  public readonly createLoanRequested = output<void>();
  public readonly removeBookRequested = output<number | undefined>();

  public trackSelectedBook(index: number, book: Book): number | string {
    return book.id ?? `book-${book.isbn ?? index}`;
  }
}

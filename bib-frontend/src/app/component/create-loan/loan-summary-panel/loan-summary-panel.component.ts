import { CommonModule } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Book } from '../../../model/book.model';

@Component({
  selector: 'app-loan-summary-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './loan-summary-panel.component.html',
  styleUrl: './loan-summary-panel.component.css',
})
export class LoanSummaryPanelComponent {
  public readonly loanForm = input.required<FormGroup>();
  public readonly minReturnDate = input.required<string>();
  public readonly selectedBooks = input.required<Book[]>();
  public readonly hasSelectedUser = input.required<boolean>();
  public readonly isSubmitting = input.required<boolean>();
  public readonly filteredBooks = input.required<Book[]>();

  public readonly createLoanRequested = output<void>();
  public readonly removeBookRequested = output<number | undefined>();
}

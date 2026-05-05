import { CommonModule } from '@angular/common';
import {
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Calendar, Clock, LucideAngularModule, X } from 'lucide-angular';
import { Loan } from '../../../../model/loan.model';
import {
  LoanExtensionRequest,
  LoanExtensionType,
} from '../../../../model/loan/loanExtension.model';
import { LocalDate } from '../../../../shared/types/local-date.type';
import { LoansService } from '../../../../services/loan/loans.service';
import { ToastService } from '../../../../services/toast/toast.service';

@Component({
  selector: 'app-extend-loan',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './extend-loan.component.html',
  styleUrl: './extend-loan.component.css',
})
export class ExtendLoanComponent {
  private readonly loansService = inject(LoansService);
  private readonly toastService = inject(ToastService);

  public readonly loanExtended = output<Loan>();
  public readonly loan = input.required<Loan>();

  public readonly cancelled = output<void>();

  protected readonly X = X;
  protected readonly Clock = Clock;
  protected readonly Calendar = Calendar;
  protected readonly isSubmitting = signal(false);

  protected readonly selectedExtensionType =
    signal<LoanExtensionType>('ONE_WEEK');
  protected readonly customReturnDate = signal<string>('');

  protected readonly isCustomDateSelected = computed(
    () => this.selectedExtensionType() === 'CUSTOM_DATE',
  );

  protected readonly minimumCustomReturnDate = computed(() => {
    const currentReturnDate = new Date(this.loan().returnDate);
    currentReturnDate.setDate(currentReturnDate.getDate() + 1);

    return currentReturnDate.toISOString().slice(0, 10);
  });

  protected readonly canSubmit = computed(() => {
    if (!this.isCustomDateSelected()) {
      return true;
    }

    return this.customReturnDate().trim().length > 0;
  });

  protected selectExtensionType(extensionType: LoanExtensionType): void {
    this.selectedExtensionType.set(extensionType);
  }

  protected updateCustomReturnDate(value: string): void {
    this.customReturnDate.set(value);
    this.selectedExtensionType.set('CUSTOM_DATE');
  }

  protected close(): void {
    this.cancelled.emit();
  }

  protected submit(): void {
    if (!this.canSubmit() || this.isSubmitting()) {
      return;
    }

    const currentLoan = this.loan();

    if (!currentLoan.id) {
      this.toastService.show('Lening kon niet gevonden worden.', 'error');
      return;
    }

    const customReturnDate = this.isCustomDateSelected()
      ? (this.customReturnDate() as LocalDate)
      : null;

    const request: LoanExtensionRequest = {
      extensionType: this.selectedExtensionType(),
      customReturnDate,
    };

    this.isSubmitting.set(true);

    this.loansService.extendLoan(currentLoan.id, request).subscribe({
      next: (updatedLoan) => {
        this.isSubmitting.set(false);
        this.toastService.show('De lening werd succesvol verlengd.', 'success');
        this.loanExtended.emit(updatedLoan);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.toastService.show('De lening kon niet verlengd worden.', 'error');
      },
    });
  }

  protected readonly loanTitle = computed(() => {
    const loan = this.loan();

    if (!loan.books || loan.books.length === 0) {
      return 'Onbekende lening';
    }

    const firstBook = loan.books[0];

    if (loan.books.length === 1) {
      return `${firstBook.title}`;
    }

    return `${firstBook.title} (+${loan.books.length - 1} boek(en))`;
  });

  protected readonly loanOwnerName = computed(() => {
    const displayName = this.loan().userDisplayName?.trim();

    if (!displayName) {
      return 'Onbekende gebruiker';
    }

    return displayName;
  });
}

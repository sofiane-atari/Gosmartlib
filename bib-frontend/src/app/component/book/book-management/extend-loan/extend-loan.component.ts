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
import { forkJoin } from 'rxjs';
import { Loan } from '../../../../model/loan/loan.model';
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
  public readonly loans = input<Loan[] | null>(null);

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

  protected readonly loansToExtend = computed(() => {
    const loans = this.loans();

    if (loans && loans.length > 0) {
      return loans;
    }

    return [this.loan()];
  });

  protected readonly isBulkExtension = computed(
    () => this.loansToExtend().length > 1,
  );

  protected readonly latestCurrentReturnDate = computed(() =>
    this.loansToExtend()
      .map((loan) => new Date(loan.returnDate))
      .sort((firstDate, secondDate) => secondDate.getTime() - firstDate.getTime())[0]
      .toISOString()
      .slice(0, 10),
  );

  protected readonly minimumCustomReturnDate = computed(() => {
    const currentReturnDate = new Date(this.latestCurrentReturnDate());
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

    const loansToExtend = this.loansToExtend();

    if (loansToExtend.some((loan) => !loan.id)) {
      this.toastService.show('Niet alle leningen konden gevonden worden.', 'error');
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

    forkJoin(
      loansToExtend.map((loan) => this.loansService.extendLoan(loan.id!, request)),
    ).subscribe({
      next: (updatedLoans) => {
        this.isSubmitting.set(false);
        this.toastService.show(
          this.isBulkExtension()
            ? 'Alle leningen werden succesvol verlengd.'
            : 'De lening werd succesvol verlengd.',
          'success',
        );
        this.loanExtended.emit(updatedLoans[0]);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.toastService.show(
          this.isBulkExtension()
            ? 'Niet alle leningen konden verlengd worden.'
            : 'De lening kon niet verlengd worden.',
          'error',
        );
      },
    });
  }

  protected readonly loanTitle = computed(() => {
    if (this.isBulkExtension()) {
      return `${this.loansToExtend().length} verlengbare leningen`;
    }

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

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import {
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  Clock,
  LibraryBig,
  LucideAngularModule,
  Calendar,
} from 'lucide-angular';

import { Loan, LoanHistory, LoanStatus } from '../../../model/loan/loan.model';
import { LoansService } from '../../../services/loan/loans.service';
import { ToastService } from '../../../services/toast/toast.service';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';

@Component({
  selector: 'app-loan-history',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, ButtonHoverHelpComponent],
  templateUrl: './loan-history.component.html',
  styleUrl: './loan-history.component.css',
})
export class LoanHistoryComponent implements OnInit {
  private readonly loansService = inject(LoansService);

  protected readonly BookOpen = BookOpen;
  protected readonly Clock = Clock;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly LibraryBig = LibraryBig;
  protected readonly Calendar = Calendar;

  protected readonly loans = signal<Loan[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly history = signal<LoanHistory | null>(null);

  private readonly toastService = inject(ToastService);

  public ngOnInit(): void {
    this.loadLoanHistory();
  }

  protected getLoanStatusLabel(status?: LoanStatus): string {
    if (status === 'RETURNED') {
      return 'Teruggebracht';
    }

    if (status === 'OVERDUE') {
      return 'Te laat';
    }

    return 'Uitgeleend';
  }

  protected getStatusClass(status?: LoanStatus): string {
    if (status === 'OVERDUE') {
      return 'status-badge status-badge--overdue';
    }

    if (status === 'RETURNED') {
      return 'status-badge status-badge--returned';
    }

    return 'status-badge status-badge--active';
  }

  private loadLoanHistory(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.loansService.getMyLoanHistory().subscribe({
      next: (history) => {
        this.history.set(history);
        this.isLoading.set(false);
      },
      error: () => {
        const message = 'Je uitleengeschiedenis kon niet geladen worden.';
        this.errorMessage.set(message);
        this.toastService.show(message, 'error');
        this.isLoading.set(false);
      },
    });
  }
}

import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AlertTriangle,
  BookOpen,
  CheckCircle2,
  Clock,
  LibraryBig,
  LucideAngularModule,
  Calendar,
} from 'lucide-angular';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

import { Loan, LoanHistory, LoanStatus } from '../../../model/loan/loan.model';
import { LoansService } from '../../../services/loan/loans.service';
import { ToastService } from '../../../services/toast/toast.service';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { PagingComponent } from '../../shared/paging/paging.component';
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";

@Component({
  selector: 'app-loan-history',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
    ButtonHoverHelpComponent,
    PagingComponent,
    StatCardComponent
],
  templateUrl: './loan-history.component.html',
  styleUrl: './loan-history.component.css',
  animations: [fadeInUpAnimation],
})
export class LoanHistoryComponent implements OnInit {
  private readonly loansService = inject(LoansService);
  private readonly toastService = inject(ToastService);

  protected readonly BookOpen = BookOpen;
  protected readonly Clock = Clock;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly LibraryBig = LibraryBig;
  protected readonly Calendar = Calendar;

  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly history = signal<LoanHistory | null>(null);

  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(10);

  protected readonly returnedLoans = computed(() => {
    return this.history()?.returnedLoans.data ?? [];
  });

  protected readonly returnedLoansMeta = computed(() => {
    return this.history()?.returnedLoans.meta ?? null;
  });

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

    this.loansService
      .getMyLoanHistory(this.currentPage(), this.pageSize())
      .subscribe({
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

  protected handleReturnedLoansPageChange(page: number): void {
    this.currentPage.set(page);
    this.loadLoanHistory();
  }
}

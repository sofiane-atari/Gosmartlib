import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  RotateCcw,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { LoansService } from '../../../services/loan/loans.service';
import { Loan } from '../../../model/loan.model';
import { Router } from '@angular/router';
import { PagingComponent, PagingMeta } from '../../shared/paging/paging.component';
import { SearchBarComponent } from "../../shared/search-bar/search-bar.component";
import { ToastService } from '../../../services/toast/toast.service';

@Component({
  selector: 'app-return-loan',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, PagingComponent, SearchBarComponent],
  templateUrl: './return-loan.component.html',
  styleUrl: './return-loan.component.css',
})
export class ReturnLoanComponent implements OnInit {
  private readonly loansService = inject(LoansService);
  private readonly router = inject(Router);

  protected readonly RotateCcw = RotateCcw;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly ArrowLeft = ArrowLeft;

  protected readonly loans = signal<Loan[]>([]);
  protected readonly searchTerm = signal('');
  protected readonly isLoading = signal(false);
  protected readonly returningLoanId = signal<number | null>(null);

  protected readonly page = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalItems = signal(0);
  protected readonly totalPages = signal(0);

  private readonly toastService = inject(ToastService);
  protected readonly expandedLoanIds = new Set<number>();
  
  protected readonly pagingMeta = computed<PagingMeta>(() => ({
    currentPage: this.page(),
    pageSize: this.pageSize(),
    totalItems: this.totalItems(),
    totalPages: this.totalPages()
  }));

  protected readonly activeLoans = computed(() =>
    this.loans().filter(loan => !loan.returnedDate),
  );

  protected readonly filteredLoans = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();

    if (!term) {
      return this.activeLoans();
    }

    return this.activeLoans().filter(loan => {
      const userName = loan.userDisplayName?.toLowerCase() ?? '';
      const userId = loan.userId?.toLowerCase() ?? '';
      const books = loan.books?.map(book => book.title?.toLowerCase() ?? '').join(' ') ?? '';

      return userName.includes(term) || userId.includes(term) || books.includes(term);
    });
  });

  public ngOnInit(): void {
    this.loadLoans();
  }

  protected goBack(): void {
    this.router.navigate(['/books-management']);
  }

  protected onPageChange(page: number): void {
    this.page.set(page);
    this.loadLoans();
  }

  protected returnLoan(loan: Loan): void {
    if (!loan.id) {
      return;
    }

    const bookCount = loan.books?.length ?? 0;
    const firstBookTitle = loan.books?.[0]?.title ?? 'deze lening';

    const message = bookCount > 1
      ? `Weet je zeker dat je deze lening met ${bookCount} boeken wilt terugbrengen?`
      : `Weet je zeker dat je "${firstBookTitle}" wilt terugbrengen?`;

    this.toastService.showConfirm(message, () => {
      this.confirmReturnLoan(loan);
    });
  }

  private loadLoans(): void {
    this.isLoading.set(true);

    this.loansService.getActiveLoans(this.page(), this.pageSize()).subscribe({
      next: (response) => {
        this.loans.set(response.data);
        this.totalItems.set(response.meta.totalItems);
        this.totalPages.set(response.meta.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.show('Leningen konden niet geladen worden.', 'error');
        this.isLoading.set(false);
      }
    });
  }
  protected getLoanStatus(loan: Loan): 'Uitgeleend' | 'Te laat' | 'Teruggebracht' {
    if (loan.returnedDate) {
      return 'Teruggebracht';
    }

    const now = new Date();
    const returnDate = new Date(loan.returnDate);
    returnDate.setHours(23, 59, 59, 999);

    if (returnDate.getTime() < now.getTime()) {
      return 'Te laat';
    }

    return 'Uitgeleend';
  }

  protected getStatusClass(loan: Loan): string {
    const status = this.getLoanStatus(loan);

    if (status === 'Te laat') {
      return 'status-badge status-badge--overdue';
    }

    if (status === 'Teruggebracht') {
      return 'status-badge status-badge--returned';
    }

    return 'status-badge status-badge--active';
  }

  protected getFirstName(displayName?: string | null): string {
    if (!displayName || displayName.trim() === '') {
      return 'Onbekende';
    }

    return displayName.trim().split(/\s+/)[0];
  }

  protected getLastName(displayName?: string | null): string {
    if (!displayName || displayName.trim() === '') {
      return 'gebruiker';
    }

    const parts = displayName.trim().split(/\s+/);

    if (parts.length === 1) {
      return '';
    }

    return parts.slice(1).join(' ');
  }

  protected firstBook(loan: Loan) {
    return loan.books?.[0] ?? null;
  }

  protected bookCount(loan: Loan): number {
    return loan.books?.length ?? 0;
  }

  protected getTimeLeftText(loan: Loan): string {
    if (loan.returnedDate) {
      return 'Afgesloten';
    }

    const now = new Date();
    const returnDate = new Date(loan.returnDate);
    returnDate.setHours(23, 59, 59, 999);

    const diffMs = returnDate.getTime() - now.getTime();

    if (diffMs <= 0) {
      const overdueMs = Math.abs(diffMs);
      const overdueHoursTotal = Math.floor(overdueMs / (1000 * 60 * 60));
      const overdueDays = Math.floor(overdueHoursTotal / 24);
      const overdueHours = overdueHoursTotal % 24;

      if (overdueDays > 0) {
        return `${overdueDays}d ${overdueHours}u te laat`;
      }

      return `${overdueHours}u te laat`;
    }

    const hoursTotal = Math.floor(diffMs / (1000 * 60 * 60));
    const days = Math.floor(hoursTotal / 24);
    const hours = hoursTotal % 24;

    if (days > 0) {
      return `${days}d ${hours}u`;
    }

    return `${hours}u`;
  }

  protected getTimeLeftClass(loan: Loan): string {
    if (loan.returnedDate) {
      return 'time-left time-left--returned';
    }

    const now = new Date();
    const returnDate = new Date(loan.returnDate);
    returnDate.setHours(23, 59, 59, 999);

    const diffMs = returnDate.getTime() - now.getTime();
    const hoursLeft = diffMs / (1000 * 60 * 60);

    if (hoursLeft <= 0) {
      return 'time-left time-left--overdue';
    }

    if (hoursLeft <= 48) {
      return 'time-left time-left--urgent';
    }

    return 'time-left';
  }

  protected toggleLoan(loanId?: number): void {
    if (loanId == null) {
      return;
    }

    if (this.expandedLoanIds.has(loanId)) {
      this.expandedLoanIds.delete(loanId);
    } else {
      this.expandedLoanIds.add(loanId);
    }
  }

  protected isLoanExpanded(loanId?: number): boolean {
    if (loanId == null) {
      return false;
    }

    return this.expandedLoanIds.has(loanId);
  }

  protected onSearchChange(value: string): void {
    this.searchTerm.set(value);
  }
  private confirmReturnLoan(loan: Loan): void {
    if (!loan.id) {
      return;
    }

    this.returningLoanId.set(loan.id);

    this.loansService.returnLoan(loan.id).subscribe({
      next: () => {
        this.toastService.show('Boek succesvol teruggebracht.', 'success');
        this.returningLoanId.set(null);
        this.loadLoans();
      },
      error: () => {
        this.toastService.show('Boek kon niet teruggebracht worden.', 'error');
        this.returningLoanId.set(null);
      },
    });
  }
}
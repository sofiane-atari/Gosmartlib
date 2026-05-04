import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  BookOpen,
  RotateCcw,
  LibraryBig,
  Clock,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
} from 'lucide-angular';
import { LoansService} from "../../../services/loan/loans.service";
import {Loan} from "../../../model/loan.model";
import { PagingMeta, PagingComponent } from '../../shared/paging/paging.component';
import { ButtonHoverHelpComponent } from "../../shared/button-hover-help/button-hover-help.component";

type LoanStatus = 'Uitgeleend' | 'Te laat' | 'Teruggebracht';

@Component({
  selector: 'app-book-management',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule, PagingComponent, ButtonHoverHelpComponent],
  templateUrl: './book-management.component.html',
  styleUrl: './book-management.component.css',
})
export class BookManagementComponent implements OnInit {
  private readonly loansService = inject(LoansService);

  protected readonly BookOpen = BookOpen;
  protected readonly RotateCcw = RotateCcw;
  protected readonly LibraryBig = LibraryBig;
  protected readonly Clock = Clock;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly ArrowRight = ArrowRight;

  protected readonly loans = signal<Loan[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly page = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalItems = signal(0);
  protected readonly totalPages = signal(0);

  expandedLoanIds = new Set<number>();

  pagingMeta = computed<PagingMeta>(() => ({
    currentPage: this.page(),
    pageSize: this.pageSize(),
    totalItems: this.totalItems(),
    totalPages: this.totalPages()
  }));

  onPageChange(page: number): void {
    this.page.set(page);
    this.expandedLoanIds.clear();
    this.loadLoans();
  }

  protected readonly activeLoans = computed(() =>
    this.loans().filter((loan) => this.getLoanStatus(loan) === 'Uitgeleend').length,
  );

  protected readonly overdueLoans = computed(() =>
    this.loans().filter((loan) => this.getLoanStatus(loan) === 'Te laat').length,
  );

  protected readonly returnedLoans = computed(() =>
    this.loans().filter((loan) => this.getLoanStatus(loan) === 'Teruggebracht').length,
  );

  public ngOnInit(): void {
    this.loadLoans();
  }

  protected getLoanStatus(loan: Loan): LoanStatus {
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
  
  loadLoans(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.loansService.getLoans(this.page(), this.pageSize()).subscribe({
      next: (response) => {
      this.loans.set(response.data);
      this.totalItems.set(response.meta.totalItems);
      this.totalPages.set(response.meta.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Leningen konden niet geladen worden.');
        this.isLoading.set(false);
      }
    });
  }

  getFirstName(displayName?: string | null): string {
    if (!displayName || displayName.trim() === '') {
      return 'Onbekende';
    }

    return displayName.trim().split(/\s+/)[0];
  }

  getLastName(displayName?: string | null): string {
    if (!displayName || displayName.trim() === '') {
      return 'gebruiker';
    }

    const parts = displayName.trim().split(/\s+/);

    if (parts.length === 1) {
      return '';
    }

    return parts.slice(1).join(' ');
  }

  hiddenBookCount(loan: any): number {
    const count = loan.books?.length ?? 0;
    return count > 2 ? count - 2 : 0;
  }

  getTimeLeftText(loan: any): string {
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

  getTimeLeftClass(loan: any): string {
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


  toggleLoan(loanId?: number): void {
    if (loanId == null) {
      return;
    }

    if (this.expandedLoanIds.has(loanId)) {
      this.expandedLoanIds.delete(loanId);
    } else {
      this.expandedLoanIds.add(loanId);
    }
  }

  isLoanExpanded(loanId?: number): boolean {
    if (loanId == null) {
      return false;
    }

    return this.expandedLoanIds.has(loanId);
  }


  firstBook(loan: any) {
    return loan.books?.[0] ?? null;
  }

  bookCount(loan: any): number {
    return loan.books?.length ?? 0;
  }
}

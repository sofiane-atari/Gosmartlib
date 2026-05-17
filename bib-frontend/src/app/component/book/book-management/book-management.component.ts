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
import { LoansService } from '../../../services/loan/loans.service';
import { Loan } from '../../../model/loan.model';
import {
  PagingMeta,
  PagingComponent,
} from '../../shared/paging/paging.component';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { ExtendLoanComponent } from './extend-loan/extend-loan.component';
import { ToastService } from '../../../services/toast/toast.service';

type LoanStatus = 'Uitgeleend' | 'Te laat' | 'Teruggebracht';

type UserLoanGroup = {
  userId: string;
  userDisplayName: string;
  loans: Loan[];
};


@Component({
  selector: 'app-book-management',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LucideAngularModule,
    PagingComponent,
    ButtonHoverHelpComponent,
    ExtendLoanComponent,
  ],
  templateUrl: './book-management.component.html',
  styleUrl: './book-management.component.css',
})
export class BookManagementComponent implements OnInit {
  private readonly loansService = inject(LoansService);
  private readonly toastService = inject(ToastService);

  protected readonly selectedLoanToExtend = signal<Loan | null>(null);

  protected readonly BookOpen = BookOpen;
  protected readonly RotateCcw = RotateCcw;
  protected readonly LibraryBig = LibraryBig;
  protected readonly Clock = Clock;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly ArrowRight = ArrowRight;

  protected readonly loans = signal<Loan[]>([]);
  protected readonly isLoading = signal(false);

  protected readonly activeLoansCount = signal(0);
  protected readonly overdueLoansCount = signal(0);
  protected readonly returnedLoansCount = signal(0);

  protected readonly activeLoans = computed(() => this.activeLoansCount());
  protected readonly overdueLoans = computed(() => this.overdueLoansCount());
  protected readonly returnedLoans = computed(() => this.returnedLoansCount());

  protected readonly page = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalItems = signal(0);
  protected readonly totalPages = signal(0);

  expandedUserIds = new Set<string>();

  pagingMeta = computed<PagingMeta>(() => ({
    currentPage: this.page(),
    pageSize: this.pageSize(),
    totalItems: this.totalItems(),
    totalPages: this.totalPages(),
  }));

  protected readonly groupedLoans = computed<UserLoanGroup[]>(() => {
    const groups = new Map<string, UserLoanGroup>();

    for (const loan of this.loans()) {
      const userKey = loan.userId ?? loan.userDisplayName ?? 'unknown-user';

      const existingGroup = groups.get(userKey);

      if (existingGroup) {
        existingGroup.loans.push(loan);
        continue;
      }

      groups.set(userKey, {
        userId: userKey,
        userDisplayName: loan.userDisplayName ?? 'Onbekende gebruiker',
        loans: [loan],
      });
    }

    return Array.from(groups.values());
  });

  onPageChange(page: number): void {
    this.page.set(page);
    this.expandedUserIds.clear();
    this.loadLoans();
  }

  public ngOnInit(): void {
    this.loadLoans();
    this.loadLoanStats();
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

    this.loansService.getLoans(this.page(), this.pageSize()).subscribe({
      next: (response) => {
        this.loans.set(response.data);
        this.totalItems.set(response.meta.totalItems);
        this.totalPages.set(response.meta.totalPages);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.show('Leningen konden niet geladen worden.', 'error');
        this.isLoading.set(false);
      },
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

  toggleUserGroup(userId: string): void {
    if (this.expandedUserIds.has(userId)) {
      this.expandedUserIds.delete(userId);
    } else {
      this.expandedUserIds.add(userId);
    }
  }

  isUserGroupExpanded(userId: string): boolean {
    return this.expandedUserIds.has(userId);
  }

  firstBook(loan: any) {
    return loan.books?.[0] ?? null;
  }

  bookCount(loan: any): number {
    return loan.books?.length ?? 0;
  }

  loadLoanStats(): void {
    this.loansService.getLoanStats().subscribe({
      next: (stats) => {
        this.activeLoansCount.set(stats.activeLoans);
        this.overdueLoansCount.set(stats.overdueLoans);
        this.returnedLoansCount.set(stats.returnedLoans);
      },
      error: () => {
        this.toastService.show('Statistieken konden niet geladen worden.', 'error');
      },
    });
  }

  protected openExtendLoanModal(loan: Loan, event: MouseEvent): void {
    event.stopPropagation();

    if (loan.returnedDate) {
      this.toastService.show(
        'Een teruggebrachte lening kan niet verlengd worden.',
        'error',
      );
      return;
    }

    if ((loan.extensionCount ?? 0) >= 2) {
      this.toastService.show(
        'Deze lening werd al maximaal twee keer verlengd.',
        'error',
      );
      return;
    }

    this.selectedLoanToExtend.set(loan);
  }

  protected closeExtendLoanModal(): void {
    this.selectedLoanToExtend.set(null);
  }

  protected handleLoanExtended(updatedLoan: Loan): void {
    this.loans.update((loans) =>
      loans.map((currentLoan) =>
        currentLoan.id === updatedLoan.id ? updatedLoan : currentLoan,
      ),
    );

    this.selectedLoanToExtend.set(null);
    this.loadLoans();
    this.loadLoanStats();
  }

  protected canExtendLoan(loan: Loan): boolean {
      return !loan.returnedDate && (loan.extensionCount ?? 0) < 2;
    }
    getGroupLoanCount(group: UserLoanGroup): number {
    return group.loans.length;
  }

  getGroupBookCount(group: UserLoanGroup): number {
    return group.loans.reduce(
      (total, loan) => total + this.bookCount(loan),
      0,
    );
  }

  getGroupFirstLoan(group: UserLoanGroup): Loan | null {
    return group.loans[0] ?? null;
  }

  getGroupStatus(group: UserLoanGroup): LoanStatus {
    if (group.loans.some((loan) => this.getLoanStatus(loan) === 'Te laat')) {
      return 'Te laat';
    }

    if (group.loans.every((loan) => this.getLoanStatus(loan) === 'Teruggebracht')) {
      return 'Teruggebracht';
    }

    return 'Uitgeleend';
  }

  getGroupStatusClass(group: UserLoanGroup): string {
    const status = this.getGroupStatus(group);

    if (status === 'Te laat') {
      return 'status-badge status-badge--overdue';
    }

    if (status === 'Teruggebracht') {
      return 'status-badge status-badge--returned';
    }

    return 'status-badge status-badge--active';
  }

  getGroupReturnDate(group: UserLoanGroup): string | null {
    const activeLoans = group.loans.filter((loan) => !loan.returnedDate);

    if (activeLoans.length === 0) {
      return group.loans[0]?.returnDate ?? null;
    }

    return activeLoans
      .map((loan) => loan.returnDate)
      .sort()[0] ?? null;
  }

  getGroupTimeLeftText(group: UserLoanGroup): string {
    const loan = this.getMostUrgentLoan(group);

    if (!loan) {
      return 'Geen leningen';
    }

    return this.getTimeLeftText(loan);
  }

  getGroupTimeLeftClass(group: UserLoanGroup): string {
    const loan = this.getMostUrgentLoan(group);

    if (!loan) {
      return 'time-left';
    }

    return this.getTimeLeftClass(loan);
  }

  private getMostUrgentLoan(group: UserLoanGroup): Loan | null {
    const activeLoans = group.loans.filter((loan) => !loan.returnedDate);

    if (activeLoans.length === 0) {
      return group.loans[0] ?? null;
    }

    return [...activeLoans].sort((a, b) =>
      new Date(a.returnDate).getTime() - new Date(b.returnDate).getTime(),
    )[0];
  }
}

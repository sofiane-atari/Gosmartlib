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
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';
import { LoansService } from '../../../services/loan/loans.service';
import { Loan, LoanBook, LoanBookCopy } from '../../../model/loan/loan.model';
import {
  PagingMeta,
  PagingComponent,
} from '../../shared/paging/paging.component';
import { ButtonHoverHelpComponent } from '../../shared/button-hover-help/button-hover-help.component';
import { ExtendLoanComponent } from './extend-loan/extend-loan.component';
import { ToastService } from '../../../services/toast/toast.service';
import { LoanGroup, LoanGroupStatus } from '../../../model/loan/loanGroup.model';
import { StatCardComponent } from "../../shared/stat-card/stat-card.component";
import { SearchBarComponent } from '../../shared/search-bar/search-bar.component';

type LoanStatus = 'Uitgeleend' | 'Te laat' | 'Teruggebracht';


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
    StatCardComponent,
    SearchBarComponent,
],
  templateUrl: './book-management.component.html',
  styleUrl: './book-management.component.css',
  animations: [fadeInUpAnimation],
})
export class BookManagementComponent implements OnInit {
  private readonly loansService = inject(LoansService);
  private readonly toastService = inject(ToastService);

  protected readonly selectedLoanToExtend = signal<Loan | null>(null);
  protected readonly selectedLoansToExtend = signal<Loan[] | null>(null);

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
  protected readonly searchTerm = signal('');
  protected readonly statusFilter = signal<LoanGroupStatus | 'ALL'>('ALL');
  protected readonly extendableFilter = signal<boolean | null>(null);

  protected readonly statusFilterOptions: { value: LoanGroupStatus | 'ALL'; label: string }[] = [
    { value: 'ALL', label: 'Alle statussen' },
    { value: 'ACTIVE', label: 'Uitgeleend' },
    { value: 'OVERDUE', label: 'Te laat' },
    { value: 'RETURNED', label: 'Teruggebracht' },
  ];

  protected readonly extendableFilterOptions: { value: string; label: string }[] = [
    { value: 'ALL', label: 'Alle leningen' },
    { value: 'EXTENDABLE', label: 'Verlengbaar' },
    { value: 'NOT_EXTENDABLE', label: 'Niet verlengbaar' },
  ];

  protected readonly loanGroups = signal<LoanGroup[]>([]);
  expandedUserIds = new Set<string>();

  pagingMeta = computed<PagingMeta>(() => ({
    currentPage: this.page(),
    pageSize: this.pageSize(),
    totalItems: this.totalItems(),
    totalPages: this.totalPages(),
  }));

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

    this.loansService
      .getGroupedLoans(
        this.page(),
        this.pageSize(),
        this.searchTerm(),
        this.statusFilter(),
        this.extendableFilter(),
      )
      .subscribe({
      next: (response) => {
        this.loanGroups.set(response.data);
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

  protected onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.resetToFirstPageAndLoad();
  }

  protected onStatusFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as LoanGroupStatus | 'ALL';
    this.statusFilter.set(value);
    this.resetToFirstPageAndLoad();
  }

  protected onExtendableFilterChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    const filter = value === 'EXTENDABLE' ? true : value === 'NOT_EXTENDABLE' ? false : null;

    this.extendableFilter.set(filter);
    this.resetToFirstPageAndLoad();
  }

  private resetToFirstPageAndLoad(): void {
    this.page.set(0);
    this.expandedUserIds.clear();
    this.loadLoans();
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

  protected getBookCopies(book: LoanBook): LoanBookCopy[] {
    if (book.copies && book.copies.length > 0) {
      return book.copies;
    }

    return (book.bookCopyIds ?? []).map((id) => ({ id, identifier: null }));
  }

  protected getLoanCopyLabels(loan: Loan): string[] {
    return loan.books
      ?.flatMap((book) => this.getBookCopies(book))
      .map((copy) => `Exemplaar ${copy.identifier?.trim() || `#${copy.id}`}`) ?? [];
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
    this.selectedLoansToExtend.set(null);
  }

  protected openExtendAllLoansModal(group: LoanGroup, event: MouseEvent): void {
    event.stopPropagation();

    const extendableLoans = this.getExtendableLoans(group);

    if (extendableLoans.length === 0) {
      this.toastService.show('Er zijn geen leningen die verlengd kunnen worden.', 'error');
      return;
    }

    this.selectedLoanToExtend.set(extendableLoans[0]);
    this.selectedLoansToExtend.set(extendableLoans);
  }

  protected closeExtendLoanModal(): void {
    this.selectedLoanToExtend.set(null);
    this.selectedLoansToExtend.set(null);
  }

  protected handleLoanExtended(updatedLoan: Loan): void {
    this.loans.update((loans) =>
      loans.map((currentLoan) =>
        currentLoan.id === updatedLoan.id ? updatedLoan : currentLoan,
      ),
    );

    this.selectedLoanToExtend.set(null);
    this.selectedLoansToExtend.set(null);
    this.loadLoans();
    this.loadLoanStats();
  }
  
  protected canExtendLoan(loan: Loan): boolean {
    return !loan.returnedDate && (loan.extensionCount ?? 0) < 2;
  }

  protected getExtendableLoans(group: LoanGroup): Loan[] {
    return [...group.loans]
      .filter((loan) => this.canExtendLoan(loan))
      .sort(
        (firstLoan, secondLoan) =>
          new Date(firstLoan.returnDate).getTime() -
          new Date(secondLoan.returnDate).getTime(),
      );
  }

  protected getDirectExtendLoan(group: LoanGroup): Loan | null {
    return this.getExtendableLoans(group)[0] ?? null;
  }

  getGroupLoanCount(group: LoanGroup): number {
    return group.loanCount;
  }

  getGroupBookCount(group: LoanGroup): number {
    return group.bookCount;
  }

  getGroupStatus(group: LoanGroup): LoanStatus {
    if (group.groupStatus === 'OVERDUE') {
      return 'Te laat';
    }

    if (group.groupStatus === 'RETURNED') {
      return 'Teruggebracht';
    }

    return 'Uitgeleend';
  }

  getGroupStatusClass(group: LoanGroup): string {
    const status = this.getGroupStatus(group);

    if (status === 'Te laat') {
      return 'status-badge status-badge--overdue';
    }

    if (status === 'Teruggebracht') {
      return 'status-badge status-badge--returned';
    }

    return 'status-badge status-badge--active';
  }

  getDisplayDate(loan: Loan): string | null {
    return loan.returnedDate ?? loan.returnDate ?? null;
  }

  getDisplayDateLabel(loan: Loan): string {
    return loan.returnedDate ? 'Teruggebracht op' : 'Retourdatum';
  }

  getGroupDisplayDate(group: LoanGroup): string | null {
    if (group.groupStatus !== 'RETURNED') {
      return group.earliestReturnDate;
    }

    return group.loans
      .map((loan) => loan.returnedDate)
      .filter((date): date is NonNullable<Loan['returnedDate']> => Boolean(date))
      .sort((a, b) => new Date(b).getTime() - new Date(a).getTime())[0] ?? null;
  }

  getGroupDisplayDateLabel(group: LoanGroup): string {
    return group.groupStatus === 'RETURNED' ? 'Teruggebracht op' : 'Retourdatum';
  }

  getGroupTimeLeftText(group: LoanGroup): string {
    const loan = this.getMostUrgentLoan(group);

    if (!loan) {
      return 'Geen leningen';
    }

    return this.getTimeLeftText(loan);
  }

  getGroupTimeLeftClass(group: LoanGroup): string {
    const loan = this.getMostUrgentLoan(group);

    if (!loan) {
      return 'time-left';
    }

    return this.getTimeLeftClass(loan);
  }

  private getMostUrgentLoan(group: LoanGroup): Loan | null {
    const activeLoans = group.loans.filter((loan) => !loan.returnedDate);

    if (activeLoans.length === 0) {
      return group.loans[0] ?? null;
    }

    return [...activeLoans].sort((a, b) =>
      new Date(a.returnDate).getTime() - new Date(b.returnDate).getTime(),
    )[0];
  }
}

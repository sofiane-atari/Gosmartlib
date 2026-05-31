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
import { forkJoin } from 'rxjs';
import { LoansService } from '../../../services/loan/loans.service';
import { Loan, LoanBook, LoanBookCopy } from '../../../model/loan/loan.model';
import { LoanGroup } from '../../../model/loan/loanGroup.model';
import { BookCondition } from '../../../model/book/book.model';
import { Router } from '@angular/router';
import { PagingComponent, PagingMeta } from '../../shared/paging/paging.component';
import { SearchBarComponent } from "../../shared/search-bar/search-bar.component";
import { ToastService } from '../../../services/toast/toast.service';
import { fadeInUpAnimation } from '../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-return-loan',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, PagingComponent, SearchBarComponent],
  templateUrl: './return-loan.component.html',
  styleUrl: './return-loan.component.css',
  animations: [fadeInUpAnimation],
})
export class ReturnLoanComponent implements OnInit {
  private readonly loansService = inject(LoansService);
  private readonly router = inject(Router);

  protected readonly RotateCcw = RotateCcw;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly ArrowLeft = ArrowLeft;

  protected readonly loans = signal<Loan[]>([]);
  protected readonly loanGroups = signal<LoanGroup[]>([]);
  protected readonly searchTerm = signal('');
  protected readonly isLoading = signal(false);
  protected readonly returningLoanId = signal<number | null>(null);
  protected readonly returningUserId = signal<string | null>(null);
  protected readonly returnConditions = signal<Record<number, BookCondition>>({});
  protected readonly conditionOptions: { value: BookCondition; label: string }[] = [
    { value: 'GOOD_CONDITION', label: 'In goede staat' },
    { value: 'DAMAGED', label: 'Beschadigd' },
    { value: 'LOST', label: 'Verloren' },
  ];

  protected readonly page = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalItems = signal(0);
  protected readonly totalPages = signal(0);

  private readonly toastService = inject(ToastService);
  protected readonly expandedLoanIds = new Set<number>();
  protected readonly expandedGroupIds = new Set<string>();

  protected readonly pagingMeta = computed<PagingMeta>(() => ({
    currentPage: this.page(),
    pageSize: this.pageSize(),
    totalItems: this.totalItems(),
    totalPages: this.totalPages()
  }));

  protected readonly filteredLoanGroups = computed<LoanGroup[]>(() => this.loanGroups());

  public ngOnInit(): void {
    this.loadLoans();
  }

  protected goBack(): void {
    this.router.navigate(['/books-management']);
  }

  protected onPageChange(page: number): void {
    this.page.set(page);
    this.expandedGroupIds.clear();
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

  protected returnAllLoans(group: LoanGroup): void {
    const loansToReturn = group.loans.filter((loan) => loan.id !== undefined);

    if (loansToReturn.length === 0) {
      return;
    }

    const message = `Weet je zeker dat je alle ${loansToReturn.length} actieve lening(en) van ${group.userDisplayName || group.userId} wilt terugbrengen?`;

    this.toastService.showConfirm(message, () => {
      this.confirmReturnAllLoans(group.groupId, group.userId, loansToReturn);
    });
  }

  private loadLoans(): void {
    this.isLoading.set(true);

    this.loansService.getActiveBatchLoans(this.page(), this.pageSize(), this.searchTerm()).subscribe({
      next: (response) => {
        this.loanGroups.set(response.data);
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

  protected toggleGroup(groupId: string): void {
    if (this.expandedGroupIds.has(groupId)) {
      this.expandedGroupIds.delete(groupId);
    } else {
      this.expandedGroupIds.add(groupId);
    }
  }

  protected isGroupExpanded(groupId: string): boolean {
    return this.expandedGroupIds.has(groupId);
  }

  protected getGroupStatus(group: LoanGroup): 'Uitgeleend' | 'Te laat' {
    return group.groupStatus === 'OVERDUE' ? 'Te laat' : 'Uitgeleend';
  }

  protected getGroupStatusClass(group: LoanGroup): string {
    return this.getGroupStatus(group) === 'Te laat'
      ? 'status-badge status-badge--overdue'
      : 'status-badge status-badge--active';
  }

  protected getGroupTimeLeftText(group: LoanGroup): string {
    const loan = this.getMostUrgentLoan(group);
    return loan ? this.getTimeLeftText(loan) : 'Geen leningen';
  }

  protected getGroupTimeLeftClass(group: LoanGroup): string {
    const loan = this.getMostUrgentLoan(group);
    return loan ? this.getTimeLeftClass(loan) : 'time-left';
  }

  protected getPrimaryBookTitle(group: LoanGroup): string {
    const firstTitle = group.loans[0]?.books?.[0]?.title;
    return firstTitle ?? 'Geen boek gekoppeld';
  }

  protected getPrimaryBookSubtitle(group: LoanGroup): string {
    const loanCount = group.loanCount;
    const bookCount = group.bookCount;

    return `${loanCount} lening(en) · ${bookCount} boek(en)`;
  }

  protected onSearchChange(value: string): void {
    this.searchTerm.set(value);
    this.page.set(0);
    this.expandedGroupIds.clear();
    this.loadLoans();
  }

  private getMostUrgentLoan(group: LoanGroup): Loan | null {
    return [...group.loans].sort(
      (a, b) => new Date(a.returnDate).getTime() - new Date(b.returnDate).getTime(),
    )[0] ?? null;
  }

  protected getBookCopyIdsForLoan(loan: Loan): number[] {
    return this.getCopiesForLoan(loan).map((copy) => copy.id);
  }

  protected getBookCopies(book: LoanBook): LoanBookCopy[] {
    if (book.copies && book.copies.length > 0) {
      return book.copies;
    }

    return (book.bookCopyIds ?? []).map((id) => ({ id, identifier: null }));
  }

  protected getCopyDisplayIdentifier(copy: LoanBookCopy): string {
    return copy.identifier?.trim() || `#${copy.id}`;
  }

  private getCopiesForLoan(loan: Loan): LoanBookCopy[] {
    const copies = loan.books?.flatMap((book) => this.getBookCopies(book)) ?? [];

    if (copies.length > 0) {
      return copies;
    }

    return (loan.bookCopyIds ?? []).map((id) => ({ id, identifier: null }));
  }

  protected getCopyCondition(copyId: number): BookCondition {
    return this.returnConditions()[copyId] ?? 'GOOD_CONDITION';
  }

  protected onReturnConditionChange(copyId: number, event: Event): void {
    const condition = (event.target as HTMLSelectElement).value as BookCondition;
    this.returnConditions.update((current) => ({
      ...current,
      [copyId]: condition,
    }));
  }

  protected getConditionLabel(condition: BookCondition): string {
    switch (condition) {
      case 'DAMAGED':
        return 'Beschadigd';
      case 'LOST':
        return 'Verloren';
      case 'GOOD_CONDITION':
      default:
        return 'In goede staat';
    }
  }

  private confirmReturnLoan(loan: Loan): void {
    if (!loan.id) {
      return;
    }

    this.returningLoanId.set(loan.id);

    const copyConditions = this.getCopiesForLoan(loan)
      .map((copy) => ({
        bookCopyId: copy.id,
        identifier: copy.identifier,
        condition: this.getCopyCondition(copy.id),
      }));

    this.loansService.returnLoan(loan.id, { copyConditions }).subscribe({
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

  private confirmReturnAllLoans(groupId: string, userId: string, loans: Loan[]): void {
    this.returningUserId.set(userId);

    const requests = loans
      .filter((loan): loan is Loan & { id: number } => loan.id !== undefined)
      .map((loan) => {
        const copyConditions = this.getCopiesForLoan(loan)
          .map((copy) => ({
            bookCopyId: copy.id,
            identifier: copy.identifier,
            condition: this.getCopyCondition(copy.id),
          }));

        return this.loansService.returnLoan(loan.id, { copyConditions });
      });

    if (requests.length === 0) {
      this.returningUserId.set(null);
      return;
    }

    forkJoin(requests).subscribe({
      next: () => {
        this.toastService.show('Alle boeken van deze gebruiker zijn succesvol teruggebracht.', 'success');
        this.returningUserId.set(null);
        this.expandedGroupIds.delete(groupId);
        this.loadLoans();
      },
      error: () => {
        this.toastService.show('Niet alle boeken konden worden teruggebracht.', 'error');
        this.returningUserId.set(null);
      },
    });
  }
}

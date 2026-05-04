import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  RotateCcw,
  Search,
  CheckCircle2,
  AlertTriangle,
  ArrowLeft
} from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { LoansService } from '../../../services/loan/loans.service';
import { Loan } from '../../../model/loan.model';
import { Router } from '@angular/router';

@Component({
  selector: 'app-return-loan',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './return-loan.component.html',
  styleUrl: './return-loan.component.css',
})
export class ReturnLoanComponent implements OnInit {
  private readonly loansService = inject(LoansService);

  private readonly router = inject(Router);
  protected readonly RotateCcw = RotateCcw;
  protected readonly Search = Search;
  protected readonly CheckCircle2 = CheckCircle2;
  protected readonly AlertTriangle = AlertTriangle;
  protected readonly ArrowLeft = ArrowLeft;

  protected readonly loans = signal<Loan[]>([]);
  protected readonly searchTerm = signal('');
  protected readonly isLoading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly returningLoanId = signal<number | null>(null);

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
      const userId = loan.userId.toLowerCase();
      const books = loan.books?.map(book => book.title.toLowerCase()).join(' ') ?? '';

      return userName.includes(term) || userId.includes(term) || books.includes(term);
    });
  });

  public ngOnInit(): void {
    // this.loadLoans();
  }

  protected goBack(): void {
    this.router.navigate(['/books-management']);
  }
  protected returnLoan(loan: Loan): void {
    if (!loan.id) {
      return;
    }

    this.returningLoanId.set(loan.id);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.loansService.returnLoan(loan.id).subscribe({
      next: (updatedLoan) => {
        this.loans.update(loans =>
          loans.map(existingLoan =>
            existingLoan.id === updatedLoan.id ? updatedLoan : existingLoan,
          ),
        );

        this.successMessage.set('Boek succesvol teruggebracht.');
        this.returningLoanId.set(null);
      },
      error: () => {
        this.errorMessage.set('Boek kon niet teruggebracht worden.');
        this.returningLoanId.set(null);
      },
    });
  }

  // private loadLoans(): void {
  //   this.isLoading.set(true);
  //   this.errorMessage.set(null);

  //   this.loansService.getLoans(this.page(), this.pageSize()).subscribe({
  //     next: (response) => {
  //       this.loans.set(response.data);
  //       this.totalItems.set(response.meta.totalItems);
  //       this.totalPages.set(response.meta.totalPages);
  //       this.isLoading.set(false);
  //     },
  //     error: () => {
  //       this.errorMessage.set('Leningen konden niet geladen worden.');
  //       this.isLoading.set(false);
  //     }
  //   });
  // }
}

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

type LoanStatus = 'Actief uitgeleend' | 'Te laat' | 'Teruggebracht';

@Component({
  selector: 'app-book-management',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule],
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

  protected readonly activeLoans = computed(() =>
    this.loans().filter((loan) => this.getLoanStatus(loan) === 'Actief uitgeleend').length,
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
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const returnDate = new Date(loan.returnDate);
    returnDate.setHours(0, 0, 0, 0);

    if (returnDate < today) {
      return 'Te laat';
    }

    return 'Actief uitgeleend';
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

  private loadLoans(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.loansService.getAllLoans().subscribe({
      next: (loans) => {
        this.loans.set(loans);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Leningen konden niet geladen worden.');
        this.isLoading.set(false);
      },
    });
  }
}

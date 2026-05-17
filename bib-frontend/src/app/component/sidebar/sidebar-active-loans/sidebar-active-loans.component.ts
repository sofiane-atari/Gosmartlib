import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import {
  BookOpen,
  Calendar,
  LucideAngularModule,
  ChevronRight,
} from 'lucide-angular';

import { Loan } from '../../../model/loan/loan.model';
import { LoansService } from '../../../services/loan/loans.service';
import { RoleService } from '../../../services/role-service/role.service';

@Component({
  selector: 'app-sidebar-active-loans',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  templateUrl: './sidebar-active-loans.component.html',
  styleUrl: './sidebar-active-loans.component.css',
})
export class SidebarActiveLoansComponent implements OnInit {
  private readonly loansService = inject(LoansService);
  private readonly roleService = inject(RoleService);

  protected readonly BookOpen = BookOpen;
  protected readonly Calendar = Calendar;
  protected readonly ChevronRight = ChevronRight;

  protected readonly activeLoans = signal<Loan[]>([]);

  protected readonly canShow = computed(
    () => this.roleService.isStudent() || this.roleService.isTeacher(),
  );

  public ngOnInit(): void {
    if (!this.canShow()) {
      return;
    }

    this.loansService.getMyLoanHistory().subscribe({
      next: (history) => {
        this.activeLoans.set((history.activeLoans ?? []).slice(0, 2));
      },
      error: () => {
        this.activeLoans.set([]);
      },
    });
  }
}

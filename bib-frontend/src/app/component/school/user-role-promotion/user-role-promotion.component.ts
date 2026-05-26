import { Component, DestroyRef, computed, inject, input, OnDestroy, OnInit, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer } from 'rxjs';
import {
  LucideAngularModule,
  ShieldCheck,
  ShieldOff,
  ShieldPlus,
  BookOpen,
  GraduationCap,
  UserRound,
} from 'lucide-angular';

import { AuthUserDto, Role } from '../../../model/auth.model';
import { UserService } from '../../../services/user/user.service';
import { RoleService } from '../../../services/role/role.service';
import { ToastService } from '../../../services/toast/toast.service';
import { DataTableComponent } from '../../shared/data-table/data-table.component';
import { CellTemplateDirective } from '../../shared/data-table/cell-template.directive';
import { DataTableColumn } from '../../shared/data-table/data-table.types';

const POLL_DELAY_MS = 3000;
const MAX_RETRIES = 5;

@Component({
  selector: 'app-user-role-promotion',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, DataTableComponent, CellTemplateDirective],
  templateUrl: './user-role-promotion.component.html',
  styleUrl: './user-role-promotion.component.css',
})
export class UserRolePromotionComponent implements OnInit, OnDestroy {
  private readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly toastService = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly schoolId = input.required<number>();
  public readonly canPromoteToAdmin = input<boolean>(true);

  public readonly librariansCountChange = output<number>();
  public readonly skipRequested = output<void>();

  protected readonly isLoading = signal(true);
  protected readonly users = signal<AuthUserDto[]>([]);
  protected readonly isPromoting = signal<string | null>(null);

  protected readonly ShieldCheck = ShieldCheck;
  protected readonly ShieldOff = ShieldOff;
  protected readonly ShieldPlus = ShieldPlus;
  protected readonly BookOpen = BookOpen;
  protected readonly GraduationCap = GraduationCap;
  protected readonly UserRound = UserRound;

  protected readonly librariansCount = computed(() =>
    this.users().filter((u) => u.role === 'LIBRARIAN').length,
  );

  protected readonly columns: DataTableColumn[] = [
    { id: 'name', header: 'Naam', dataLabel: 'Naam' },
    { id: 'email', header: 'E-mail', dataLabel: 'E-mail' },
    { id: 'role', header: 'Huidige rol', dataLabel: 'Huidige rol' },
    { id: 'actions', header: 'Acties', dataLabel: 'Acties', align: 'right' },
  ];

  public ngOnInit(): void {
    this.loadUsers();
  }

  public ngOnDestroy(): void {}

  public loadUsers(): void {
    this.isLoading.set(true);
    this.fetchWithRetries(MAX_RETRIES);
  }

  public skip(): void {
    this.skipRequested.emit();
  }

  private fetchWithRetries(retriesLeft: number): void {
    this.userService
      .getUsersBySchoolId(this.schoolId())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (users) => {
          const sorted = [...users].sort((a, b) =>
            this.getDisplayName(a).localeCompare(this.getDisplayName(b)),
          );

          if (sorted.length > 0) {
            this.users.set(sorted);
            this.isLoading.set(false);
            this.librariansCountChange.emit(this.librariansCount());
            return;
          }

          if (retriesLeft <= 0) {
            this.isLoading.set(false);
            return;
          }

          timer(POLL_DELAY_MS)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.fetchWithRetries(retriesLeft - 1));
        },
        error: () => {
          if (retriesLeft <= 0) {
            this.isLoading.set(false);
            return;
          }

          timer(POLL_DELAY_MS)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.fetchWithRetries(retriesLeft - 1));
        },
      });
  }

  protected promoteToLibrarian(user: AuthUserDto): void {
    this.changeRole(user.id, () => this.roleService.assignLibrarian(user.id), 'LIBRARIAN');
  }

  protected promoteToAdmin(user: AuthUserDto): void {
    this.changeRole(user.id, () => this.roleService.assignAdmin(user.id), 'ADMIN');
  }

  protected demoteToTeacher(user: AuthUserDto): void {
    const role = user.role;
    const action$ =
      role === 'LIBRARIAN'
        ? this.roleService.revokeLibrarian(user.id)
        : this.roleService.revokeAdmin(user.id);

    this.changeRole(user.id, () => action$, 'TEACHER');
  }

  protected getRoleLabel(role: Role): string {
    return this.userService.getRoleLabel(role);
  }

  protected roleBadgeClass(role: Role): string {
    switch (role) {
      case 'ADMIN':
        return 'role-badge role-badge--admin';
      case 'LIBRARIAN':
        return 'role-badge role-badge--librarian';
      case 'TEACHER':
        return 'role-badge role-badge--teacher';
      default:
        return 'role-badge';
    }
  }

  protected getDisplayName(user: AuthUserDto): string {
    return this.userService.getDisplayName(user);
  }

  protected canPromote(user: AuthUserDto): boolean {
    return user.role === 'TEACHER';
  }

  protected canDemote(user: AuthUserDto): boolean {
    return user.role === 'LIBRARIAN' || user.role === 'ADMIN';
  }

  private changeRole(
    userId: string,
    actionFactory: () => ReturnType<typeof this.roleService.assignLibrarian>,
    targetRole: Role,
  ): void {
    const displayName = this.getDisplayName(
      this.users().find((u) => u.id === userId) ?? { id: userId } as AuthUserDto,
    );

    this.isPromoting.set(userId);

    actionFactory()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.users.update((current) =>
            current.map((u) =>
              u.id === updatedUser.id ? { ...u, role: updatedUser.role } : u,
            ),
          );
          this.isPromoting.set(null);
          this.librariansCountChange.emit(this.librariansCount());
          this.toastService.show(
            `${displayName} is nu ${this.getRoleLabel(targetRole).toLowerCase()}.`,
            'success',
          );
        },
        error: (error: HttpErrorResponse) => {
          this.isPromoting.set(null);
          const errMsg =
            error?.error?.error?.message ?? 'Rol kon niet gewijzigd worden.';
          this.toastService.show(errMsg, 'error');
        },
      });
  }
}

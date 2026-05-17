import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

import {
  LucideAngularModule,
  Search,
  UserCog,
  GraduationCap,
  ShieldCheck,
  ShieldOff,
  Loader,
  UserRound,
  Users,
} from 'lucide-angular';

import { UserService } from '../../services/user/user.service';
import { RoleService } from '../../services/role/role.service';
import { AuthService } from '../../services/authorization/auth.service';
import { ToastService } from '../../services/toast/toast.service';
import { AuthUserDto, Role } from '../../model/auth.model';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';
import { HttpErrorResponseError } from '../../error-reponse.model';

import { PagingComponent, PagingMeta } from '../shared/paging/paging.component';
import { StatCardComponent } from '../shared/stat-card/stat-card.component';
import { SearchBarComponent } from '../shared/search-bar/search-bar.component';
import { ConfirmationModalComponent } from '../shared/confirmation-modal/confirmation-modal.component';
import { LoadingOverlayComponent } from '../shared/loading-overlay/loading-overlay.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
    PagingComponent,
    StatCardComponent,
    SearchBarComponent,
    ConfirmationModalComponent,
    LoadingOverlayComponent,
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css'],
})
export class UserManagementComponent {
  protected readonly Search = Search;
  protected readonly UserCog = UserCog;
  protected readonly GraduationCap = GraduationCap;
  protected readonly ShieldCheck = ShieldCheck;
  protected readonly ShieldOff = ShieldOff;
  protected readonly Loader = Loader;
  protected readonly UserRound = UserRound;
  protected readonly Users = Users;

  protected readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toastService = inject(ToastService);

  protected readonly page = signal(0);
  protected readonly pageSize = signal(20);
  private readonly refreshTrigger = signal(0);
  private readonly searchSubject = new Subject<string>();
  protected readonly searchQuery = signal('');

  protected readonly selectedUser = signal<AuthUserDto | null>(null);
  protected readonly isConfirmModalOpen = signal(false);
  protected readonly isPromoting = signal(false);
  protected readonly isRevokeAction = signal(false);

  protected readonly currentUser = this.authService.currentUser;

  protected readonly canSelfRevoke = computed(() =>
    this.currentUser()?.role === 'LIBRARIAN',
  );

  constructor() {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((query) => {
        this.searchQuery.set(query);
        this.page.set(0);
      });
  }

  private readonly loadParams = computed(() => ({
    page: this.page(),
    pageSize: this.pageSize(),
    search: this.searchQuery(),
    trigger: this.refreshTrigger(),
  }));

  private readonly teachers$ = toObservable(this.loadParams).pipe(
    switchMap(({ page, pageSize, search }) =>
      this.userService.getTeachers(true, page, pageSize, search || undefined),
    ),
  );

  private readonly teachersResponse = toSignal(this.teachers$, {
    initialValue: { data: [], meta: { currentPage: 0, pageSize: 20, totalItems: 0, totalPages: 0 } } as PaginatedResponse<AuthUserDto>,
  });

  protected readonly teachers = computed(() => this.teachersResponse().data);
  protected readonly totalItems = computed(() => this.teachersResponse().meta.totalItems);
  protected readonly totalPages = computed(() => this.teachersResponse().meta.totalPages);
  protected readonly isLoading = computed(() => this.teachersResponse() === null);

  protected readonly pagingMeta = computed<PagingMeta>(() => ({
    currentPage: this.page(),
    pageSize: this.pageSize(),
    totalItems: this.totalItems(),
    totalPages: this.totalPages(),
  }));

  private readonly stats$ = toObservable(this.refreshTrigger).pipe(
    switchMap(() => this.userService.getTeacherStats()),
  );

  protected readonly stats = toSignal(this.stats$, {
    initialValue: { totalTeachers: 0, activeTeachers: 0, librarians: 0 },
  });

  protected readonly actionLabel = computed(() =>
    this.isRevokeAction() ? 'wordt teruggezet' : 'wordt gepromoveerd',
  );

  protected readonly confirmTitle = computed(() =>
    this.isRevokeAction() ? 'Bibliothecarisrol intrekken' : 'Leerkracht promoveren',
  );

  protected readonly confirmDescription = computed(() => {
    const user = this.selectedUser();

    if (!user) {
      return '';
    }

    const name = this.userService.getDisplayName(user);

    if (this.isRevokeAction()) {
      return `Weet je zeker dat je de bibliothecarisrol van "${name}" wilt intrekken? Deze gebruiker wordt teruggezet naar leerkracht.`;
    }

    return `Weet je zeker dat je "${name}" wilt promoveren tot bibliothecaris? Deze gebruiker krijgt dan beheerdersrechten binnen de bibliotheekmodule.`;
  });

  protected readonly confirmButtonLabel = computed(() =>
    this.isRevokeAction() ? 'Intrekken' : 'Promoveren',
  );

  protected readonly confirmLoadingLabel = computed(() =>
    this.isRevokeAction() ? 'Bezig met intrekken...' : 'Bezig met promoveren...',
  );

  protected roleBadgeClass(role: Role): string {
    switch (role) {
      case 'TEACHER':
        return 'role-badge role-badge--teacher';
      case 'LIBRARIAN':
        return 'role-badge role-badge--librarian';
      case 'ADMIN':
        return 'role-badge role-badge--admin';
      default:
        return 'role-badge';
    }
  }

  protected onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  protected onPageChange(page: number): void {
    this.page.set(page);
  }

  protected openSelfRevoke(): void {
    const user = this.currentUser();
    if (!user) {
      return;
    }
    this.isRevokeAction.set(true);
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected openConfirmPromote(user: AuthUserDto): void {
    this.isRevokeAction.set(false);
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected openConfirmRevoke(user: AuthUserDto): void {
    this.isRevokeAction.set(true);
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected closeConfirmModal(): void {
    this.isConfirmModalOpen.set(false);
    this.selectedUser.set(null);
  }

  protected onConfirmAction(): void {
    const user = this.selectedUser();

    if (!user) {
      return;
    }

    this.isPromoting.set(true);

    const action$ = this.isRevokeAction()
      ? this.roleService.revokeLibrarian(user.id)
      : this.roleService.assignLibrarian(user.id);

    action$.subscribe({
      next: () => {
        const message = this.isRevokeAction()
          ? `De bibliothecarisrol van ${this.userService.getDisplayName(user)} is succesvol ingetrokken.`
          : `${this.userService.getDisplayName(user)} is succesvol gepromoveerd tot bibliothecaris.`;

        this.toastService.show(message, 'success');
        this.closeConfirmModal();
        this.authService.fetchCurrentUser();
        this.refreshTrigger.update((v) => v + 1);

        if (this.isRevokeAction() && user.id === this.currentUser()?.id) {
          this.router.navigate(['/catalog']);
        }
      },
      error: (error: HttpErrorResponseError) => {
        this.handleError(error);
        this.isPromoting.set(false);
      },
      complete: () => this.isPromoting.set(false),
    });
  }

  private handleError(error: HttpErrorResponseError): void {
    const errorBody = error?.error;

    if (Array.isArray(errorBody?.details) && errorBody.details.length > 0) {
      const messages = errorBody.details
        .map((detail: { message?: string }) => detail?.message)
        .filter(Boolean)
        .join('\n');

      this.toastService.show(messages || 'Er is een fout opgetreden.', 'error');
      return;
    }

    if (errorBody?.message) {
      this.toastService.show(errorBody.message, 'error');
      return;
    }

    this.toastService.show('Er is een fout opgetreden.', 'error');
  }
}

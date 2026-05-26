import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, tap } from 'rxjs/operators';

import {
  LucideAngularModule,
  Search,
  UserCog,
  GraduationCap,
  ShieldCheck,
  BookOpen,
  ShieldOff,
  Loader,
  UserRound,
  Users,
  ShieldPlus,
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
import { ButtonHoverHelpComponent } from '../shared/button-hover-help/button-hover-help.component';
import { DataTableComponent } from '../shared/data-table/data-table.component';
import { CellTemplateDirective } from '../shared/data-table/cell-template.directive';
import { DataTableColumn } from '../shared/data-table/data-table.types';

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
    ButtonHoverHelpComponent,
    DataTableComponent,
    CellTemplateDirective,
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.css'],
})
export class UserManagementComponent {
  protected readonly Search = Search;
  protected readonly UserCog = UserCog;
  protected readonly GraduationCap = GraduationCap;
  protected readonly ShieldCheck = ShieldCheck;
  protected readonly BookOpen = BookOpen;
  protected readonly ShieldOff = ShieldOff;
  protected readonly ShieldPlus = ShieldPlus;
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

  protected readonly loading = signal(true);
  protected readonly selectedUser = signal<AuthUserDto | null>(null);
  protected readonly isConfirmModalOpen = signal(false);
  protected readonly isPromoting = signal(false);
  protected readonly isRevokeAction = signal(false);
  protected readonly actionTarget = signal<'librarian' | 'admin'>('librarian');

  protected readonly currentUser = this.authService.currentUser;

  protected readonly canManageAdmins = computed(() =>
    this.currentUser()?.role === 'ADMIN',
  );

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
    tap({ next: () => this.loading.set(false) }),
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

  protected readonly columns: DataTableColumn[] = [
    { id: 'name', header: 'Naam', dataLabel: 'Naam' },
    { id: 'email', header: 'E-mail', dataLabel: 'E-mail' },
    { id: 'role', header: 'Huidige rol', dataLabel: 'Huidige rol' },
    { id: 'actions', header: 'Acties', dataLabel: 'Acties', align: 'right' },
  ];

  private readonly stats$ = toObservable(this.refreshTrigger).pipe(
    switchMap(() => this.userService.getTeacherStats()),
    tap({ next: () => this.loading.set(false) }),
  );

  protected readonly stats = toSignal(this.stats$, {
    initialValue: { totalTeachers: 0, activeTeachers: 0, librarians: 0 },
  });

  protected readonly actionLabel = computed(() => {
    if (this.isRevokeAction()) {
      return this.actionTarget() === 'admin' ? 'wordt teruggezet als admin' : 'wordt teruggezet';
    }
    return this.actionTarget() === 'admin' ? 'wordt gepromoveerd tot admin' : 'wordt gepromoveerd';
  });

  protected readonly confirmTitle = computed(() => {
    if (this.isRevokeAction()) {
      return this.actionTarget() === 'admin' ? 'Adminrol intrekken' : 'Beheerdersrol intrekken';
    }
    return this.actionTarget() === 'admin' ? 'Leerkracht promoveren tot admin' : 'Leerkracht promoveren tot beheerder';
  });

  protected readonly confirmDescription = computed(() => {
    const user = this.selectedUser();

    if (!user) {
      return '';
    }

    const name = this.userService.getDisplayName(user);

    if (this.isRevokeAction()) {
      if (this.actionTarget() === 'admin') {
        return `Weet je zeker dat je de adminrol van "${name}" wilt intrekken? Deze gebruiker wordt teruggezet naar leerkracht.`;
      }
      return `Weet je zeker dat je de beheerdersrol van "${name}" wilt intrekken? Deze gebruiker wordt teruggezet naar leerkracht.`;
    }

    if (this.actionTarget() === 'admin') {
      return `Weet je zeker dat je "${name}" wilt promoveren tot admin? Deze gebruiker krijgt dan volledige beheerdersrechten.`;
    }
    return `Weet je zeker dat je "${name}" wilt promoveren tot beheerder? Deze gebruiker krijgt dan beheerdersrechten binnen de bibliotheekmodule.`;
  });

  protected readonly confirmButtonLabel = computed(() => {
    if (this.isRevokeAction()) {
      return this.actionTarget() === 'admin' ? 'Adminrol intrekken' : 'Intrekken';
    }
    return this.actionTarget() === 'admin' ? 'Promoveren tot admin' : 'Promoveren';
  });

  protected readonly confirmLoadingLabel = computed(() => {
    if (this.isRevokeAction()) {
      return this.actionTarget() === 'admin' ? 'Bezig met intrekken...' : 'Bezig met intrekken...';
    }
    return this.actionTarget() === 'admin' ? 'Bezig met promoveren...' : 'Bezig met promoveren...';
  });

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
    this.actionTarget.set('librarian');
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected openConfirmPromote(user: AuthUserDto): void {
    this.isRevokeAction.set(false);
    this.actionTarget.set('librarian');
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected openConfirmRevoke(user: AuthUserDto): void {
    this.isRevokeAction.set(true);
    this.actionTarget.set('librarian');
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected openConfirmPromoteToAdmin(user: AuthUserDto): void {
    this.isRevokeAction.set(false);
    this.actionTarget.set('admin');
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected openConfirmRevokeAdmin(user: AuthUserDto): void {
    this.isRevokeAction.set(true);
    this.actionTarget.set('admin');
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

    const schoolCode = this.currentUser()?.school?.domainName;

    let action$;
    if (this.actionTarget() === 'admin') {
      action$ = this.isRevokeAction()
        ? this.roleService.revokeAdmin(user.id, schoolCode)
        : this.roleService.assignAdmin(user.id, schoolCode);
    } else {
      action$ = this.isRevokeAction()
        ? this.roleService.revokeLibrarian(user.id, schoolCode)
        : this.roleService.assignLibrarian(user.id, schoolCode);
    }

    action$.subscribe({
      next: () => {
        let message: string;
        if (this.isRevokeAction()) {
          if (this.actionTarget() === 'admin') {
            message = `De adminrol van ${this.userService.getDisplayName(user)} is succesvol ingetrokken.`;
          } else {
            message = `De beheerdersrol van ${this.userService.getDisplayName(user)} is succesvol ingetrokken.`;
          }
        } else {
          if (this.actionTarget() === 'admin') {
            message = `${this.userService.getDisplayName(user)} is succesvol gepromoveerd tot admin.`;
          } else {
            message = `${this.userService.getDisplayName(user)} is succesvol gepromoveerd tot beheerder.`;
          }
        }

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

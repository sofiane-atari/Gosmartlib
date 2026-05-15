import { Component, computed, inject, signal } from '@angular/core';
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
  Loader,
} from 'lucide-angular';

import { UserService } from '../../services/user/user.service';
import { RoleService } from '../../services/role/role.service';
import { ToastService } from '../../services/toast/toast.service';
import { AuthUserDto } from '../../model/auth.model';
import { HttpErrorResponseError } from '../../error-reponse.model';

import { SearchBarComponent } from '../shared/search-bar/search-bar.component';
import { ConfirmationModalComponent } from '../shared/confirmation-modal/confirmation-modal.component';
import { LoadingOverlayComponent } from '../shared/loading-overlay/loading-overlay.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    LucideAngularModule,
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
  protected readonly Loader = Loader;

  protected readonly userService = inject(UserService);
  private readonly roleService = inject(RoleService);
  private readonly toastService = inject(ToastService);

  private readonly refreshTrigger = signal(0);
  private readonly searchSubject = new Subject<string>();
  protected readonly searchQuery = signal('');

  protected readonly selectedUser = signal<AuthUserDto | null>(null);
  protected readonly isConfirmModalOpen = signal(false);
  protected readonly isPromoting = signal(false);

  constructor() {
    this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((query) => this.searchQuery.set(query));
  }

  private readonly teachers$ = toObservable(this.refreshTrigger).pipe(
    switchMap(() => this.userService.getTeachers()),
  );

  private readonly teachersResponse = toSignal(this.teachers$, {
    initialValue: [] as AuthUserDto[],
  });

  protected readonly teachers = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const all = this.teachersResponse();

    if (!query) {
      return all;
    }

    return all.filter(
      (u) =>
        u.fullName?.toLowerCase().includes(query) ||
        u.firstName?.toLowerCase().includes(query) ||
        u.lastName?.toLowerCase().includes(query) ||
        u.email?.toLowerCase().includes(query),
    );
  });

  protected readonly isLoading = computed(() => this.teachersResponse() === null);

  protected readonly confirmDescription = computed(() => {
    const user = this.selectedUser();

    if (!user) {
      return '';
    }

    const name = this.userService.getDisplayName(user);

    return `Weet je zeker dat je "${name}" wilt promoveren tot bibliothecaris? Deze gebruiker krijgt dan beheerdersrechten binnen de bibliotheekmodule.`;
  });

  protected onSearch(query: string): void {
    this.searchSubject.next(query);
  }

  protected openConfirmPromote(user: AuthUserDto): void {
    this.selectedUser.set(user);
    this.isConfirmModalOpen.set(true);
  }

  protected closeConfirmPromote(): void {
    this.isConfirmModalOpen.set(false);
    this.selectedUser.set(null);
  }

  protected onConfirmPromote(): void {
    const user = this.selectedUser();

    if (!user) {
      return;
    }

    this.isPromoting.set(true);

    this.roleService.assignLibrarian(user.id).subscribe({
      next: () => {
        this.toastService.show(
          `${this.userService.getDisplayName(user)} is succesvol gepromoveerd tot bibliothecaris.`,
          'success',
        );
        this.closeConfirmPromote();
        this.refreshTrigger.update((v) => v + 1);
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

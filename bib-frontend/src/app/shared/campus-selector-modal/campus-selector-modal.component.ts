import { CommonModule } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideAngularModule, Building2, Check } from 'lucide-angular';

import { Campus} from "../../model/campus.model";
import { AuthService} from "../../services/authorization/auth.service";
import { CampusService} from "../../services/campus/campus.service";

@Component({
  selector: 'app-campus-selector-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './campus-selector-modal.component.html',
  styleUrl: './campus-selector-modal.component.css',
})
export class CampusSelectorModalComponent {
  private readonly authService = inject(AuthService);
  private readonly campusService = inject(CampusService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly Building2 = Building2;
  protected readonly Check = Check;

  protected readonly campuses = signal<Campus[]>([]);
  protected readonly isLoading = signal(false);
  protected readonly isSaving = signal(false);
  protected readonly isOpen = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  private lastCheckedUserKey: string | null = null;

  protected readonly currentUser = computed(() => this.authService.currentUser());

  protected readonly currentCampusId = computed(() => this.currentUser()?.campusId ?? null);

  public constructor() {
    this.authService.currentUserEffect((user) => {
      if (!user?.id || !user.schoolId) {
        this.closeWithoutMarking();
        return;
      }

      const userKey = `${user.id}:${user.schoolId}`;

      if (this.lastCheckedUserKey === userKey) {
        return;
      }

      this.lastCheckedUserKey = userKey;

      if (this.hasCampusPromptBeenHandled(userKey)) {
        this.closeWithoutMarking();
        return;
      }

      this.checkCampuses(userKey);
    });
  }

  protected selectCampus(campus: Campus): void {
    const user = this.currentUser();

    if (!user?.id || !user.schoolId) {
      return;
    }

    const userKey = `${user.id}:${user.schoolId}`;
    this.markCampusPromptHandled(userKey);

    if (this.isSaving() || campus.id === this.currentCampusId()) {
      this.closeWithoutMarking();
      return;
    }

    this.isSaving.set(true);
    this.errorMessage.set(null);

    this.campusService
      .switchMyCampus(campus.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.authService.currentUser.set(updatedUser);
          this.isSaving.set(false);
          this.closeWithoutMarking();

          window.location.reload();
        },
        error: () => {
          sessionStorage.removeItem(this.getCampusPromptStorageKey(userKey));
          this.errorMessage.set('Campus kon niet gewijzigd worden. Probeer opnieuw.');
          this.isSaving.set(false);
        },
      });
  }

  protected close(): void {
    const user = this.currentUser();

    if (user?.id && user.schoolId) {
      this.markCampusPromptHandled(`${user.id}:${user.schoolId}`);
    }

    this.closeWithoutMarking();
  }

  private closeWithoutMarking(): void {
    this.isOpen.set(false);
    this.errorMessage.set(null);
  }

  private checkCampuses(userKey: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.campusService
      .getForCurrentUserSchool()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (campuses) => {
          const sortedCampuses = [...campuses].sort(
            (a, b) =>
              Number(b.defaultCampus) - Number(a.defaultCampus) ||
              a.name.localeCompare(b.name),
          );

          this.campuses.set(sortedCampuses);
          this.isLoading.set(false);

          if (sortedCampuses.length > 1 && !this.hasCampusPromptBeenHandled(userKey)) {
            this.isOpen.set(true);
          } else {
            this.closeWithoutMarking();
          }
        },
        error: () => {
          this.isLoading.set(false);
          this.closeWithoutMarking();
        },
      });
  }

  private hasCampusPromptBeenHandled(userKey: string): boolean {
    return sessionStorage.getItem(this.getCampusPromptStorageKey(userKey)) === 'true';
  }

  private markCampusPromptHandled(userKey: string): void {
    sessionStorage.setItem(this.getCampusPromptStorageKey(userKey), 'true');
  }

  private getCampusPromptStorageKey(userKey: string): string {
    return `campusPromptHandled:${userKey}`;
  }
}

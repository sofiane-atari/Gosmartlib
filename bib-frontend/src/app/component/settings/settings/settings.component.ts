import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { UserSettingsService } from '../../../services/user-settings/user-settings.service';
import { CampusService } from '../../../services/campus/campus.service';
import { AuthService } from '../../../services/authorization/auth.service';
import { Campus } from '../../../model/campus.model';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.css'],
})
export class SettingsComponent {
  public readonly userSettingsService = inject(UserSettingsService);

  private readonly campusService = inject(CampusService);
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly campuses = signal<Campus[]>([]);
  protected readonly isLoadingCampuses = signal(false);
  protected readonly isSavingCampus = signal(false);
  protected readonly campusError = signal<string | null>(null);
  protected readonly campusSuccess = signal<string | null>(null);

  protected readonly currentUser = this.authService.currentUser;

  public constructor() {
    this.loadCampuses();
  }

  toggleHoverHelp(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.userSettingsService.setHoverHelpEnabled(input.checked);
  }

  protected changeCampus(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const campusId = Number(select.value);

    if (!campusId || campusId === this.currentUser()?.campusId) {
      return;
    }

    this.isSavingCampus.set(true);
    this.campusError.set(null);
    this.campusSuccess.set(null);

    this.campusService
      .switchMyCampus(campusId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (updatedUser) => {
          this.authService.currentUser.set(updatedUser);
          this.isSavingCampus.set(false);
          this.campusSuccess.set('Campus succesvol gewijzigd. De catalogus wordt opnieuw geladen.');

          setTimeout(() => {
            window.location.reload();
          }, 700);
        },
        error: () => {
          this.isSavingCampus.set(false);
          this.campusError.set('Campus kon niet gewijzigd worden. Probeer opnieuw.');
        },
      });
  }

  private loadCampuses(): void {
    if (!this.authService.isAuthenticated()) {
      return;
    }

    this.isLoadingCampuses.set(true);
    this.campusError.set(null);

    this.campusService
      .getForCurrentUserSchool()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (campuses) => {
          this.campuses.set(
            [...campuses].sort(
              (a, b) =>
                Number(b.defaultCampus) - Number(a.defaultCampus) ||
                a.name.localeCompare(b.name),
            ),
          );
          this.isLoadingCampuses.set(false);
        },
        error: () => {
          this.isLoadingCampuses.set(false);
          this.campusError.set('Campussen konden niet geladen worden.');
        },
      });
  }
}

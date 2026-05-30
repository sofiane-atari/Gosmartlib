import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { OneRosterSyncService } from '../../services/oneroster/oneroster-sync.service';
import { UserSettingsService } from '../../services/user-settings/user-settings.service';
import { AuthService } from '../../services/authorization/auth.service';
import { SchoolService } from '../../services/school/school.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
})
export class SettingsComponent {
  private readonly syncService = inject(OneRosterSyncService);
  private readonly authService = inject(AuthService);
  private readonly schoolService = inject(SchoolService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly userSettingsService = inject(UserSettingsService);

  protected readonly currentUser = this.authService.currentUser;

  protected readonly isSyncing = signal(false);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly catalogVisible = signal(false);
  protected readonly catalogVisToggling = signal(false);

  protected syncNow(): void {
    if (this.isSyncing()) {
      return;
    }

    this.isSyncing.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    this.syncService
      .triggerSync()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSyncing.set(false);
          this.successMessage.set('De synchronisatie is gestart. Dit kan even duren.');
        },
        error: () => {
          this.isSyncing.set(false);
          this.errorMessage.set(
            'De synchronisatie kon niet gestart worden. Probeer opnieuw.',
          );
        },
      });
  }

  protected toggleHoverHelp(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.userSettingsService.setHoverHelpEnabled(input.checked);
  }
  
  protected toggleCatalogFeaturedLists(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.userSettingsService.setCatalogFeaturedListsEnabled(input.checked);
  }

  protected toggleCatalogMostRead(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.userSettingsService.setCatalogMostReadEnabled(input.checked);
  }

  protected toggleCatalogNewest(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.userSettingsService.setCatalogNewestEnabled(input.checked);
  }

  protected toggleCatalogVisibility(event: Event): void {
    const input = event.target as HTMLInputElement;
    const schoolId = this.currentUser()?.schoolId;

    if (!schoolId) {
      input.checked = !input.checked;
      return;
    }

    this.catalogVisToggling.set(true);

    this.schoolService.setCatalogVisibility(schoolId, input.checked).subscribe({
      next: () => {
        this.catalogVisible.set(input.checked);
        this.catalogVisToggling.set(false);
      },
      error: () => {
        input.checked = !input.checked;
        this.catalogVisToggling.set(false);
      },
    });
  }

  constructor() {
      effect(() => {
        const visible = this.currentUser()?.school?.catalogVisible;
        if (visible !== undefined && visible !== null) {
          this.catalogVisible.set(visible);
        }
      }, { allowSignalWrites: true });
  }
}

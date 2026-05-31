import { Component, DestroyRef, inject, signal, OnInit, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { OneRosterSyncService } from '../../services/oneroster/oneroster-sync.service';
import { UserSettingsService } from '../../services/user-settings/user-settings.service';
import { AuthService } from '../../services/authorization/auth.service';
import { SchoolService } from '../../services/school/school.service';
import { DevSeedService } from '../../services/dev-seed/dev-seed.service';
import { DevFeatureWrapperComponent } from '../shared/dev-feature-wrapper/dev-feature-wrapper.component';
import { fadeInUpAnimation } from '../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, DevFeatureWrapperComponent],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
  animations: [fadeInUpAnimation],
})
export class SettingsComponent implements OnInit {
  private readonly syncService = inject(OneRosterSyncService);
  private readonly devSeedService = inject(DevSeedService);
  private readonly authService = inject(AuthService);
  private readonly schoolService = inject(SchoolService);
  private readonly destroyRef = inject(DestroyRef);

  public readonly userSettingsService = inject(UserSettingsService);

  protected readonly currentUser = this.authService.currentUser;

  protected readonly isSyncing = signal(false);
  protected readonly isSeeding = signal(false);
  protected readonly isClearing = signal(false);
  protected readonly syncSuccessMessage = signal<string | null>(null);
  protected readonly syncErrorMessage = signal<string | null>(null);
  protected readonly seedSuccessMessage = signal<string | null>(null);
  protected readonly seedErrorMessage = signal<string | null>(null);
  protected readonly clearSuccessMessage = signal<string | null>(null);
  protected readonly clearErrorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.userSettingsService.loadCatalogSidebarSettings();
  }

  protected readonly catalogVisible = signal(false);
  protected readonly catalogVisToggling = signal(false);

  protected syncNow(): void {
    if (this.isSyncing()) {
      return;
    }

    this.isSyncing.set(true);
    this.syncSuccessMessage.set(null);
    this.syncErrorMessage.set(null);

    this.syncService
      .triggerSync()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.isSyncing.set(false);
          this.syncSuccessMessage.set('De synchronisatie is gestart. Dit kan even duren.');
        },
        error: () => {
          this.isSyncing.set(false);
          this.syncErrorMessage.set(
            'De synchronisatie kon niet gestart worden. Probeer opnieuw.',
          );
        },
      });
  }

  protected triggerDevSeed(): void {
    if (this.isSeeding()) {
      return;
    }

    this.isSeeding.set(true);
    this.seedSuccessMessage.set(null);
    this.seedErrorMessage.set(null);

    this.devSeedService
      .triggerSeed()
      .then((result) => {
        this.isSeeding.set(false);
        this.seedSuccessMessage.set(
          `Dev-seed voltooid: ${result.booksCreated} boeken, ${result.copiesCreated} exemplaren, ${result.loansCreated} leningen, ${result.reviewsCreated} reviews, ${result.readingListsCreated} leeslijsten, ${result.lessonTipsCreated} lestips, ${result.tagsCreated} tags aangemaakt.`,
        );
      })
      .catch((err) => {
        this.isSeeding.set(false);
        if (err.status === 403) {
          this.seedErrorMessage.set('Dev-seed is alleen beschikbaar in development-modus.');
        } else {
          this.seedErrorMessage.set('Dev-seed kon niet uitgevoerd worden. Probeer opnieuw.');
        }
      });
  }

  protected clearDevSeed(): void {
    if (this.isClearing()) {
      return;
    }

    this.isClearing.set(true);
    this.clearSuccessMessage.set(null);
    this.clearErrorMessage.set(null);

    this.devSeedService
      .clearSeed()
      .then(() => {
        this.isClearing.set(false);
        this.clearSuccessMessage.set('Dev-seed data werd succesvol gewist.');
      })
      .catch((err) => {
        this.isClearing.set(false);
        if (err.status === 403) {
          this.clearErrorMessage.set('Dev-seed is alleen beschikbaar in development-modus.');
        } else if (err.status === 412) {
          this.clearErrorMessage.set('Geen seed-data om te wissen. Voer eerst een dev-seed uit.');
        } else {
          this.clearErrorMessage.set('Dev-seed data kon niet gewist worden. Probeer opnieuw.');
        }
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

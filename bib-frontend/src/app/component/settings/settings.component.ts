import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OneRosterSyncService } from '../../services/oneroster/oneroster-sync.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
})
export class SettingsComponent {
  private readonly syncService = inject(OneRosterSyncService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly isSyncing = signal(false);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly errorMessage = signal<string | null>(null);

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
}

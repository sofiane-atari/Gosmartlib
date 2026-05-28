import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule,
  ChevronDown,
  ChevronUp,
  HelpCircle,
} from 'lucide-angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { OneRosterSyncService } from '../../../services/oneroster/oneroster-sync.service';
import { SyncConfig } from '../../../model/sync-config.model';

@Component({
  selector: 'app-oneroster-help',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './oneroster-help.component.html',
  styleUrl: './oneroster-help.component.css',
})
export class OnerosterHelpComponent {
  private readonly syncService = inject(OneRosterSyncService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly isExpanded = signal(false);
  protected readonly syncConfig = signal<SyncConfig | null>(null);
  protected readonly loadError = signal(false);
  protected readonly HelpCircle = HelpCircle;
  protected readonly ChevronDown = ChevronDown;
  protected readonly ChevronUp = ChevronUp;

  protected toggle(): void {
    const expanded = !this.isExpanded();
    this.isExpanded.set(expanded);

    if (expanded && !this.syncConfig()) {
      this.syncService.getConfig().pipe(
        takeUntilDestroyed(this.destroyRef),
      ).subscribe({
        next: (config) => this.syncConfig.set(config),
        error: () => this.loadError.set(true),
      });
    }
  }
}

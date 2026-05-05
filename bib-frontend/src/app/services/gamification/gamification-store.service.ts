import { computed, inject, Injectable, signal } from '@angular/core';

import {
  GamificationBadge,
  GamificationProfile,
  GamificationProgress,
  GamificationRecentEvent,
} from '../../model/gamification/gamification.model';
import { GamificationService } from './gamification.service';

@Injectable({
  providedIn: 'root',
})
export class GamificationStoreService {
  private readonly gamificationService = inject(GamificationService);

  private readonly profileSignal = signal<GamificationProfile | null>(null);
  private readonly isLoadingSignal = signal<boolean>(false);
  private readonly errorMessageSignal = signal<string | null>(null);

  public readonly profile = this.profileSignal.asReadonly();
  public readonly isLoading = this.isLoadingSignal.asReadonly();
  public readonly errorMessage = this.errorMessageSignal.asReadonly();

  public readonly totalPoints = computed<number>(() => {
    return this.profileSignal()?.totalPoints ?? 0;
  });

  public readonly currentLevel = computed<number>(() => {
    return this.profileSignal()?.currentLevel ?? 1;
  });

  public readonly pointsUntilNextLevel = computed<number>(() => {
    return this.profileSignal()?.pointsUntilNextLevel ?? 0;
  });

  public readonly latestAchievements = computed<GamificationBadge[]>(() => {
    return this.profileSignal()?.latestAchievements ?? [];
  });

  public readonly earnedBadges = computed<GamificationBadge[]>(() => {
    return this.profileSignal()?.earnedBadges ?? [];
  });

  public readonly badgeProgress = computed<GamificationProgress[]>(() => {
    return this.profileSignal()?.badgeProgress ?? [];
  });

  public readonly recentEvents = computed<GamificationRecentEvent[]>(() => {
    return this.profileSignal()?.recentEvents ?? [];
  });

  public loadCurrentUserProfile(): void {
    this.isLoadingSignal.set(true);
    this.errorMessageSignal.set(null);

    this.gamificationService.getCurrentUserProfile().subscribe({
      next: (profile: GamificationProfile) => {
        this.profileSignal.set(profile);
        this.isLoadingSignal.set(false);
      },
      error: () => {
        this.profileSignal.set(null);
        this.errorMessageSignal.set(
          'Gamificatiegegevens konden niet worden geladen.',
        );
        this.isLoadingSignal.set(false);
      },
    });
  }

  public refreshCurrentUserProfile(): void {
    this.loadCurrentUserProfile();
  }

  public clearProfile(): void {
    this.profileSignal.set(null);
    this.errorMessageSignal.set(null);
    this.isLoadingSignal.set(false);
  }
}

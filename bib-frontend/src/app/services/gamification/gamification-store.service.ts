import { computed, inject, Injectable, signal } from '@angular/core';

import {
  GamificationBadge,
  GamificationProfile,
  GamificationProgress,
  GamificationRecentEvent,
} from '../../model/gamification/gamification.model';
import {
  GamificationBadgeFilters,
  GamificationService,
} from './gamification.service';

@Injectable({
  providedIn: 'root',
})
export class GamificationStoreService {
  private readonly gamificationService = inject(GamificationService);

  private readonly profileSignal = signal<GamificationProfile | null>(null);
  private readonly isLoadingSignal = signal<boolean>(false);
  private readonly errorMessageSignal = signal<string | null>(null);

  private readonly badgesSignal = signal<GamificationProgress[]>([]);
  private readonly badgesLoadingSignal = signal<boolean>(false);
  private readonly badgesErrorMessageSignal = signal<string | null>(null);

  public readonly badges = this.badgesSignal.asReadonly();
  public readonly badgesLoading = this.badgesLoadingSignal.asReadonly();
  public readonly badgesErrorMessage =
    this.badgesErrorMessageSignal.asReadonly();

  public readonly profile = this.profileSignal.asReadonly();
  public readonly isLoading = this.isLoadingSignal.asReadonly();
  public readonly errorMessage = this.errorMessageSignal.asReadonly();

  private readonly almostEarnedBadgesSignal = signal<GamificationProgress[]>(
    [],
  );
  private readonly almostEarnedBadgesLoadingSignal = signal<boolean>(false);
  private readonly almostEarnedBadgesErrorMessageSignal = signal<string | null>(
    null,
  );

  public readonly almostEarnedBadges =
    this.almostEarnedBadgesSignal.asReadonly();
  public readonly almostEarnedBadgesLoading =
    this.almostEarnedBadgesLoadingSignal.asReadonly();
  public readonly almostEarnedBadgesErrorMessage =
    this.almostEarnedBadgesErrorMessageSignal.asReadonly();

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

  public loadCurrentUserBadges(filters: GamificationBadgeFilters = {}): void {
    this.badgesLoadingSignal.set(true);
    this.badgesErrorMessageSignal.set(null);

    this.gamificationService.getCurrentUserBadges(filters).subscribe({
      next: (badges: GamificationProgress[]) => {
        this.badgesSignal.set(badges);
        this.badgesLoadingSignal.set(false);
      },
      error: () => {
        this.badgesSignal.set([]);
        this.badgesErrorMessageSignal.set('Badges konden niet worden geladen.');
        this.badgesLoadingSignal.set(false);
      },
    });
  }

  public loadCurrentUserAlmostEarnedBadges(): void {
    this.almostEarnedBadgesLoadingSignal.set(true);
    this.almostEarnedBadgesErrorMessageSignal.set(null);

    this.gamificationService.getCurrentUserAlmostEarnedBadges().subscribe({
      next: (badges: GamificationProgress[]) => {
        this.almostEarnedBadgesSignal.set(badges);
        this.almostEarnedBadgesLoadingSignal.set(false);
      },
      error: () => {
        this.almostEarnedBadgesSignal.set([]);
        this.almostEarnedBadgesErrorMessageSignal.set(
          'Badges die bijna behaald zijn konden niet worden geladen.',
        );
        this.almostEarnedBadgesLoadingSignal.set(false);
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

  public refreshCurrentUserBadges(
    filters: GamificationBadgeFilters = {},
  ): void {
    this.loadCurrentUserBadges(filters);
  }

  public clearBadges(): void {
    this.badgesSignal.set([]);
    this.badgesErrorMessageSignal.set(null);
    this.badgesLoadingSignal.set(false);
  }
}

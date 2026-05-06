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
import { PaginatedMeta } from '../../model/shared/paging/PaginatedMeta.model.ts';
import { PageingOpt } from '../../model/shared/paging/PageingOpt.model';
import { PaginatedResponse } from '../../model/shared/paging/PaginatedResponse.model';

@Injectable({
  providedIn: 'root',
})
export class GamificationStoreService {
  private static readonly DEFAULT_RECENT_EVENTS_PAGE = 0;
  private static readonly DEFAULT_RECENT_EVENTS_PAGE_SIZE = 5;

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

  private readonly recentEventsSignal = signal<GamificationRecentEvent[]>([]);
  private readonly recentEventsMetaSignal = signal<PaginatedMeta | null>(null);
  private readonly recentEventsLoadingSignal = signal<boolean>(false);
  private readonly recentEventsErrorMessageSignal = signal<string | null>(null);

  public readonly pagedRecentEvents = this.recentEventsSignal.asReadonly();
  public readonly recentEventsMeta = this.recentEventsMetaSignal.asReadonly();
  public readonly recentEventsLoading =
    this.recentEventsLoadingSignal.asReadonly();
  public readonly recentEventsErrorMessage =
    this.recentEventsErrorMessageSignal.asReadonly();

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

  public readonly levelProgressPercentage = computed<number>(() => {
    const totalPoints = this.totalPoints();
    const pointsUntilNextLevel = this.pointsUntilNextLevel();
    const pointsInCurrentLevel = totalPoints % 100;
    const levelSize = pointsInCurrentLevel + pointsUntilNextLevel;

    if (levelSize <= 0) {
      return 100;
    }

    return Math.min(Math.round((pointsInCurrentLevel / levelSize) * 100), 100);
  });

  public loadCurrentUserRecentEvents(
    options: PageingOpt = {
      page: GamificationStoreService.DEFAULT_RECENT_EVENTS_PAGE,
      pageSize: GamificationStoreService.DEFAULT_RECENT_EVENTS_PAGE_SIZE,
    },
  ): void {
    this.recentEventsLoadingSignal.set(true);
    this.recentEventsErrorMessageSignal.set(null);

    this.gamificationService.getCurrentUserRecentEvents(options).subscribe({
      next: (response: PaginatedResponse<GamificationRecentEvent>) => {
        this.recentEventsSignal.set(response.data);
        this.recentEventsMetaSignal.set(response.meta);
        this.recentEventsLoadingSignal.set(false);
      },
      error: () => {
        this.recentEventsSignal.set([]);
        this.recentEventsMetaSignal.set(null);
        this.recentEventsErrorMessageSignal.set(
          'Recente activiteit kon niet worden geladen.',
        );
        this.recentEventsLoadingSignal.set(false);
      },
    });
  }

  public clearRecentEvents(): void {
    this.recentEventsSignal.set([]);
    this.recentEventsMetaSignal.set(null);
    this.recentEventsErrorMessageSignal.set(null);
    this.recentEventsLoadingSignal.set(false);
  }

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

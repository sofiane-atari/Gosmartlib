import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  GamificationBadgeCategory,
  GamificationBadgeRarity,
  GamificationProfile,
  GamificationProgress,
} from '../../model/gamification/gamification.model';

export interface GamificationBadgeFilters {
  category?: GamificationBadgeCategory;
  rarity?: GamificationBadgeRarity;
  completed?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class GamificationService {
  private readonly profileApiUrl = `${environment.apiUrl}/gamification/me`;
  private readonly badgesApiUrl = `${environment.apiUrl}/gamification/me/badges`;
  private readonly almostEarnedBadgesApiUrl = `${environment.apiUrl}/gamification/me/badges/almost-earned`;

  private readonly httpClient = inject(HttpClient);

  public getCurrentUserProfile(): Observable<GamificationProfile> {
    return this.httpClient.get<GamificationProfile>(this.profileApiUrl);
  }

  public getCurrentUserBadges(
    filters: GamificationBadgeFilters = {},
  ): Observable<GamificationProgress[]> {
    let params = new HttpParams();

    if (filters.category !== undefined) {
      params = params.set('category', filters.category);
    }

    if (filters.rarity !== undefined) {
      params = params.set('rarity', filters.rarity);
    }

    if (filters.completed !== undefined) {
      params = params.set('completed', String(filters.completed));
    }

    return this.httpClient.get<GamificationProgress[]>(this.badgesApiUrl, {
      params,
    });
  }

  public getCurrentUserAlmostEarnedBadges(): Observable<
    GamificationProgress[]
  > {
    return this.httpClient.get<GamificationProgress[]>(
      this.almostEarnedBadgesApiUrl,
    );
  }
}

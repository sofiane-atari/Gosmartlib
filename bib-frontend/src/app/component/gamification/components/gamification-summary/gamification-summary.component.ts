import { Component, inject, OnInit } from '@angular/core';
import { GamificationStoreService } from '../../../../services/gamification/gamification-store.service';
import { BadgeCardComponent } from '../badge-card/badge-card.component';
import { Target, Award, Star, LucideAngularModule } from 'lucide-angular';
import { AlmostEarnedBadgesComponent } from '../almost-earned-badges/almost-earned-badges.component';
import { RecentGamificationEventsComponent } from '../recent-gamification-events/recent-gamification-events.component';

@Component({
  selector: 'app-gamification-summary',
  standalone: true,
  imports: [
    BadgeCardComponent,
    LucideAngularModule,
    AlmostEarnedBadgesComponent,
    RecentGamificationEventsComponent,
  ],
  templateUrl: './gamification-summary.component.html',
  styleUrl: './gamification-summary.component.css',
})
export class GamificationSummaryComponent implements OnInit {
  protected readonly gamificationStore = inject(GamificationStoreService);
  private static readonly RECENT_EVENTS_PAGE_SIZE = 5;

  protected readonly Target = Target;
  protected readonly Award = Award;
  protected readonly Star = Star;

  public ngOnInit(): void {
    this.gamificationStore.loadCurrentUserProfile();
    this.gamificationStore.loadCurrentUserAlmostEarnedBadges();
    this.gamificationStore.loadCurrentUserRecentEvents();
  }

  protected onRecentEventsPageChanged(page: number): void {
    this.gamificationStore.loadCurrentUserRecentEvents({
      page,
      pageSize: GamificationSummaryComponent.RECENT_EVENTS_PAGE_SIZE,
    });
  }
}

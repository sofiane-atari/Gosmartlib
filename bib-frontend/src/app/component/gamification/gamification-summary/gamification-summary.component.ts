import { Component, inject, OnInit } from '@angular/core';
import { GamificationStoreService } from '../../../services/gamification/gamification-store.service';
import { BadgeCardComponent } from '../badge-card/badge-card.component';
import { BadgeProgressCardComponent } from '../badge-progress-card/badge-progress-card.component';
import { Target, Award, Star, LucideAngularModule } from 'lucide-angular';
import { BadgeCollectionComponent } from '../badge-collection/badge-collection.component';
import { GamificationBadgeFilters } from '../../../services/gamification/gamification.service';
import { AlmostEarnedBadgesComponent } from '../almost-earned-badges/almost-earned-badges.component';

@Component({
  selector: 'app-gamification-summary',
  standalone: true,
  imports: [
    BadgeCardComponent,
    BadgeProgressCardComponent,
    LucideAngularModule,
    BadgeCollectionComponent,
    AlmostEarnedBadgesComponent,
  ],
  templateUrl: './gamification-summary.component.html',
  styleUrl: './gamification-summary.component.css',
})
export class GamificationSummaryComponent implements OnInit {
  protected readonly gamificationStore = inject(GamificationStoreService);

  protected readonly Target = Target;
  protected readonly Award = Award;
  protected readonly Star = Star;

  public ngOnInit(): void {
    this.gamificationStore.loadCurrentUserProfile();
    this.gamificationStore.loadCurrentUserBadges();
    this.gamificationStore.loadCurrentUserAlmostEarnedBadges();
  }

  protected onBadgeFiltersChanged(filters: GamificationBadgeFilters): void {
    this.gamificationStore.loadCurrentUserBadges(filters);
  }
}

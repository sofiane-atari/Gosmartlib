import { Component, OnInit, inject } from '@angular/core';

import { BadgeCollectionComponent } from '../../components/badge-collection/badge-collection.component';
import { GamificationTabsComponent } from '../../components/gamification-tabs/gamification-tabs.component';
import { GamificationBadgeFilters } from '../../../../services/gamification/gamification.service';
import { GamificationStoreService } from '../../../../services/gamification/gamification-store.service';
import { fadeInUpAnimation } from '../../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-badge-page',
  standalone: true,
  imports: [BadgeCollectionComponent, GamificationTabsComponent],
  templateUrl: './badge-page.component.html',
  styleUrl: './badge-page.component.css',
  animations: [fadeInUpAnimation],
})
export class BadgePageComponent implements OnInit {
  protected readonly gamificationStore = inject(GamificationStoreService);

  public ngOnInit(): void {
    this.gamificationStore.loadCurrentUserBadges();
  }

  protected onBadgeFiltersChanged(filters: GamificationBadgeFilters): void {
    this.gamificationStore.loadCurrentUserBadges(filters);
  }
}

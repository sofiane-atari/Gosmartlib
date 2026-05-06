import { Component, input, output } from '@angular/core';

import { PagingComponent } from '../../../shared/paging/paging.component';
import { GamificationRecentEvent } from '../../../../model/gamification/gamification.model.js';
import { PaginatedMeta } from '../../../../model/shared/paging/PaginatedMeta.model.ts';
import { ButtonHoverHelpComponent } from '../../../shared/button-hover-help/button-hover-help.component';

@Component({
  selector: 'app-recent-gamification-events',
  standalone: true,
  imports: [PagingComponent, ButtonHoverHelpComponent],
  templateUrl: './recent-gamification-events.component.html',
  styleUrl: './recent-gamification-events.component.css',
})
export class RecentGamificationEventsComponent {
  public readonly events = input.required<GamificationRecentEvent[]>();
  public readonly meta = input<PaginatedMeta | null>(null);
  public readonly isLoading = input<boolean>(false);
  public readonly errorMessage = input<string | null>(null);

  public readonly pageChanged = output<number>();

  protected getPointsLabel(event: GamificationRecentEvent): string {
    return event.points > 0 ? `+${event.points}` : `${event.points}`;
  }

  protected getEventTypeLabel(type: string): string {
    switch (type) {
      case 'BOOK_RETURNED':
        return 'Boek teruggebracht';
      case 'BOOK_RETURNED_ON_TIME':
        return 'Op tijd teruggebracht';
      case 'REVIEW_CREATED':
        return 'Recensie';
      case 'BADGE_REWARD':
        return 'Badgebeloning';
      default:
        return 'Activiteit';
    }
  }

  protected onPageChanged(page: number): void {
    this.pageChanged.emit(page);
  }
}

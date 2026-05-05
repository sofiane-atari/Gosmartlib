import { Component, input } from '@angular/core';

import { BadgeProgressCardComponent } from '../badge-progress-card/badge-progress-card.component';
import { GamificationProgress } from '../../../../model/gamification/gamification.model';

@Component({
  selector: 'app-almost-earned-badges',
  standalone: true,
  imports: [BadgeProgressCardComponent],
  templateUrl: './almost-earned-badges.component.html',
  styleUrl: './almost-earned-badges.component.css',
})
export class AlmostEarnedBadgesComponent {
  public readonly badges = input.required<GamificationProgress[]>();
  public readonly isLoading = input<boolean>(false);
  public readonly errorMessage = input<string | null>(null);
}

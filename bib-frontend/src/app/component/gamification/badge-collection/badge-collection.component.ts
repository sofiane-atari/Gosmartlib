import { Component, input } from '@angular/core';

import { BadgeProgressCardComponent } from '../badge-progress-card/badge-progress-card.component';
import { GamificationProgress } from '../../../model/gamification/gamification.model';

@Component({
  selector: 'app-badge-collection',
  standalone: true,
  imports: [BadgeProgressCardComponent],
  templateUrl: './badge-collection.component.html',
  styleUrl: './badge-collection.component.css',
})
export class BadgeCollectionComponent {
  public readonly badges = input.required<GamificationProgress[]>();
}

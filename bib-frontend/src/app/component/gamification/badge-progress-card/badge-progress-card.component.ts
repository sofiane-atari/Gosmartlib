import { Component, input } from '@angular/core';
import { GamificationProgress } from '../../../model/gamification/gamification.model';

@Component({
  selector: 'app-badge-progress-card',
  standalone: true,
  templateUrl: './badge-progress-card.component.html',
  styleUrl: './badge-progress-card.component.css',
})
export class BadgeProgressCardComponent {
  public readonly progress = input.required<GamificationProgress>();

  protected get progressLabel(): string {
    return `${this.progress().currentValue} / ${this.progress().targetValue}`;
  }

  protected get badgeInitial(): string {
    return this.progress().badgeName.charAt(0).toUpperCase();
  }

  protected get badgeAssetPath(): string {
    const assetKey = this.progress().completed
      ? this.progress().assetKey
      : this.progress().lockedAssetKey;

    return `assets/gamification/badges/${assetKey}.svg`;
  }

  protected get badgeAltText(): string {
    return this.progress().completed
      ? this.progress().badgeName
      : `Vergrendelde badge: ${this.progress().badgeName}`;
  }
}

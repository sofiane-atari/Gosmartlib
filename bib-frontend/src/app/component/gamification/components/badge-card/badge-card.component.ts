import { Component, input } from '@angular/core';
import { GamificationBadge } from '../../../../model/gamification/gamification.model';

@Component({
  selector: 'app-badge-card',
  standalone: true,
  templateUrl: './badge-card.component.html',
  styleUrl: './badge-card.component.css',
})
export class BadgeCardComponent {
  public readonly badge = input.required<GamificationBadge>();

  protected get badgeInitial(): string {
    return this.badge().name.charAt(0).toUpperCase();
  }

  protected get badgeAssetPath(): string {
    return `assets/gamification/badges/${this.badge().assetKey}.svg`;
  }

  protected get rarityLabel(): string {
    switch (this.badge().rarity) {
      case 'COMMON':
        return 'Basis';
      case 'RARE':
        return 'Zeldzaam';
      case 'EPIC':
        return 'Episch';
      case 'LEGENDARY':
        return 'Legendarisch';
      default:
        return 'Onbekend';
    }
  }
}

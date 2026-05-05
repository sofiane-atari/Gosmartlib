import { Component, input, signal } from '@angular/core';
import { GamificationProgress } from '../../../model/gamification/gamification.model';

@Component({
  selector: 'app-badge-collection-card',
  standalone: true,
  templateUrl: './badge-collection-card.component.html',
  styleUrl: './badge-collection-card.component.css',
})
export class BadgeCollectionCardComponent {
  public readonly badge = input.required<GamificationProgress>();

  protected readonly hasImageError = signal<boolean>(false);

  protected get badgeAssetPath(): string {
    const assetKey = this.badge().completed
      ? this.badge().assetKey
      : this.badge().lockedAssetKey;

    return `/assets/gamification/badges/${assetKey}.svg`;
  }

  protected get badgeAltText(): string {
    return this.badge().completed
      ? this.badge().badgeName
      : `Vergrendelde badge: ${this.badge().badgeName}`;
  }

  protected get badgeInitial(): string {
    return this.badge().badgeName.charAt(0).toUpperCase();
  }

  protected get statusLabel(): string {
    return this.badge().completed ? 'Behaald' : 'Nog te behalen';
  }

  protected get rarityLabel(): string {
    switch (this.badge().rarity) {
      case 'COMMON':
        return 'Gewoon';
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

  protected get rarityClass(): string {
    return `badge-collection-card--${this.badge().rarity.toLowerCase()}`;
  }

  protected markImageAsFailed(): void {
    this.hasImageError.set(true);
  }
}

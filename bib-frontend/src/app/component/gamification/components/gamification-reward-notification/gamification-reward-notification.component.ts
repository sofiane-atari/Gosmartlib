import { Component, input, output, signal } from '@angular/core';

import { GamificationNotification } from '../../../../model/gamification/gamification-notification.model';

@Component({
  selector: 'app-gamification-reward-notification',
  standalone: true,
  templateUrl: './gamification-reward-notification.component.html',
  styleUrl: './gamification-reward-notification.component.css',
})
export class GamificationRewardNotificationComponent {
  public readonly notification = input.required<GamificationNotification>();

  public readonly dismissed = output<void>();

  protected readonly hasImageError = signal<boolean>(false);

  protected get badgeAssetPath(): string {
    return `/assets/gamification/badges/${this.notification().assetKey}.svg`;
  }

  protected get rarityLabel(): string {
    switch (this.notification().rarity) {
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

  protected get badgeInitial(): string {
    return this.notification().title.charAt(0).toUpperCase();
  }

  protected markImageAsFailed(): void {
    this.hasImageError.set(true);
  }

  protected closeNotification(): void {
    this.dismissed.emit();
  }
}

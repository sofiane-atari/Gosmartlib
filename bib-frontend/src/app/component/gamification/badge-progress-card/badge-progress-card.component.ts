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
}

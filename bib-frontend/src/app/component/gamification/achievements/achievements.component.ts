import { Component } from '@angular/core';
import { GamificationSummaryComponent } from '../gamification-summary/gamification-summary.component';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [GamificationSummaryComponent],
  templateUrl: './achievements.component.html',
  styleUrl: './achievements.component.css',
})
export class AchievementsComponent {}

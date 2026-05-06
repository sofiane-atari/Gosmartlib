import { Component } from '@angular/core';
import { GamificationSummaryComponent } from '../../components/gamification-summary/gamification-summary.component';
import { GamificationTabsComponent } from '../../components/gamification-tabs/gamification-tabs.component';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [GamificationSummaryComponent, GamificationTabsComponent],
  templateUrl: './achievements.component.html',
  styleUrl: './achievements.component.css',
})
export class AchievementsComponent {}

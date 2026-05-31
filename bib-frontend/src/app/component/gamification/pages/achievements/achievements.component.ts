import { Component } from '@angular/core';
import { GamificationSummaryComponent } from '../../components/gamification-summary/gamification-summary.component';
import { GamificationTabsComponent } from '../../components/gamification-tabs/gamification-tabs.component';
import { fadeInUpAnimation } from '../../../shared/animations/my-reviews.animations';

@Component({
  selector: 'app-achievements',
  standalone: true,
  imports: [GamificationSummaryComponent, GamificationTabsComponent],
  templateUrl: './achievements.component.html',
  styleUrl: './achievements.component.css',
  animations: [fadeInUpAnimation],
})
export class AchievementsComponent {}

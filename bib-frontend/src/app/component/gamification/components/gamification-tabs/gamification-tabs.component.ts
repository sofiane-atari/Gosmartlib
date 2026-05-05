import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-gamification-tabs',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './gamification-tabs.component.html',
  styleUrl: './gamification-tabs.component.css',
})
export class GamificationTabsComponent {}

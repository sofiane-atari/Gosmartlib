import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from './component/sidebar/sidebar.component';
import { ToastComponent } from './component/shared/toast/toast.component';
import { AuthService } from './services/authorization/auth.service';
import { SessionExpiryModalComponent } from './component/shared/session-expired-modal/session-expired-modal.component';
import { GamificationRewardNotificationComponent } from './component/gamification/components/gamification-reward-notification/gamification-reward-notification.component';
import { GamificationNotificationStoreService } from './services/gamification/store/gamification-notification-store.service';
import { CampusSelectorModalComponent } from './shared/campus-selector-modal/campus-selector-modal.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    SidebarComponent,
    RouterOutlet,
    ToastComponent,
    SessionExpiryModalComponent,
    GamificationRewardNotificationComponent,
    CampusSelectorModalComponent,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  protected readonly authService = inject(AuthService);
  protected readonly gamificationNotificationStore = inject(
    GamificationNotificationStoreService,
  );

  protected isSidebarOpen = signal(false);

  cards = [
    'Catalogus',
    'Favorietenlijst',
    'Mijn Account',
    'Instellingen',
    'BookBox',
    'Rapporteer',
  ];

  public constructor() {
    if (this.authService.isAuthenticated()) {
      this.authService.fetchCurrentUser();
      this.gamificationNotificationStore.loadUnreadNotifications();
    }
  }

  toggleSidebar(): void {
    this.isSidebarOpen.update((v) => !v);
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
  }
}

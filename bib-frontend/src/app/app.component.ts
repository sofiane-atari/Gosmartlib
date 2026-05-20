import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { SidebarComponent } from './component/sidebar/sidebar.component';
import { ToastComponent } from './component/shared/toast/toast.component';
import { AuthService } from './services/authorization/auth.service';
import { SessionExpiryModalComponent } from './component/shared/session-expired-modal/session-expired-modal.component';
import { GamificationRewardNotificationComponent } from './component/gamification/components/gamification-reward-notification/gamification-reward-notification.component';
import { GamificationNotificationStoreService } from './services/gamification/notification/gamification-notification-store.service';
import { GamificationNotificationStreamService } from './services/gamification/notification/gamification-notification-stream.service';

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
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  protected readonly authService = inject(AuthService);
  protected readonly gamificationNotificationStore = inject(
    GamificationNotificationStoreService,
  );

  private readonly gamificationNotificationStream = inject(
    GamificationNotificationStreamService,
  );

  protected isSidebarOpen = signal(false);

  cards = [
    'Catalogus',
    'Favorietenlijst',
    'Mijn Account',
    'Instellingen',
    'Rapporteer',
  ];

  private notificationStreamStarted = false;

  public constructor() {
    effect(
      () => {
        if (this.authService.isAuthenticated()) {
          if (this.notificationStreamStarted) {
            return;
          }

          this.notificationStreamStarted = true;

          this.authService.fetchCurrentUser();
          this.gamificationNotificationStore.loadUnreadNotifications();
          this.gamificationNotificationStream.connect();

          return;
        }

        this.notificationStreamStarted = false;
        this.gamificationNotificationStream.disconnect();
        this.gamificationNotificationStore.clearNotifications();
      },
      { allowSignalWrites: true },
    );
  }

  public ngOnDestroy(): void {
    this.gamificationNotificationStream.disconnect();
  }

  toggleSidebar(): void {
    this.isSidebarOpen.update((v) => !v);
  }

  closeSidebar(): void {
    this.isSidebarOpen.set(false);
  }
}

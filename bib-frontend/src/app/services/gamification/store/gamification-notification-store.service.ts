import { inject, Injectable, signal } from '@angular/core';
import { GamificationService } from '../gamification.service';
import { GamificationNotification } from '../../../model/gamification/gamification-notification.model';

@Injectable({
  providedIn: 'root',
})
export class GamificationNotificationStoreService {
  private readonly gamificationService = inject(GamificationService);

  private readonly notificationsSignal = signal<GamificationNotification[]>([]);
  private readonly activeNotificationSignal =
    signal<GamificationNotification | null>(null);
  private readonly isLoadingSignal = signal<boolean>(false);
  private readonly errorMessageSignal = signal<string | null>(null);

  public readonly notifications = this.notificationsSignal.asReadonly();
  public readonly activeNotification =
    this.activeNotificationSignal.asReadonly();
  public readonly isLoading = this.isLoadingSignal.asReadonly();
  public readonly errorMessage = this.errorMessageSignal.asReadonly();

  public loadUnreadNotifications(): void {
    this.isLoadingSignal.set(true);
    this.errorMessageSignal.set(null);

    this.gamificationService.getUnreadNotifications().subscribe({
      next: (notifications: GamificationNotification[]) => {
        this.notificationsSignal.set(notifications);
        this.activeNotificationSignal.set(notifications[0] ?? null);
        this.isLoadingSignal.set(false);
      },
      error: () => {
        this.notificationsSignal.set([]);
        this.activeNotificationSignal.set(null);
        this.errorMessageSignal.set(
          'Beloningsmeldingen konden niet worden geladen.',
        );
        this.isLoadingSignal.set(false);
      },
    });
  }

  public dismissActiveNotification(): void {
    const activeNotification = this.activeNotificationSignal();

    if (activeNotification === null) {
      return;
    }

    this.gamificationService
      .markNotificationAsRead(activeNotification.id)
      .subscribe({
        next: () => {
          this.removeNotification(activeNotification.id);
        },
        error: () => {
          this.removeNotification(activeNotification.id);
        },
      });
  }

  public clearNotifications(): void {
    this.notificationsSignal.set([]);
    this.activeNotificationSignal.set(null);
    this.errorMessageSignal.set(null);
    this.isLoadingSignal.set(false);
  }

  private removeNotification(notificationId: number): void {
    const remainingNotifications = this.notificationsSignal().filter(
      (notification) => notification.id !== notificationId,
    );

    this.notificationsSignal.set(remainingNotifications);
    this.activeNotificationSignal.set(remainingNotifications[0] ?? null);
  }
}

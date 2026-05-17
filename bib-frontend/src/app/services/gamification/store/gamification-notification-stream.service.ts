import { inject, Injectable } from '@angular/core';

import { fetchEventSource } from '@microsoft/fetch-event-source';
import { GamificationNotificationStoreService } from './gamification-notification-store.service';
import { environment } from '../../../../environments/environment';
import { GamificationNotification } from '../../../model/gamification/gamification-notification.model';
import { AuthService } from '../../authorization/auth.service';

@Injectable({
  providedIn: 'root',
})
export class GamificationNotificationStreamService {
  private readonly streamUrl = `${environment.apiUrl}/gamification/me/notifications/stream`;

  private readonly notificationStore = inject(
    GamificationNotificationStoreService,
  );

  private readonly authService = inject(AuthService);

  private abortController: AbortController | null = null;

  public connect(): void {
    if (this.abortController !== null) {
      return;
    }

    const token = this.authService.getValidToken();

    if (token === null) {
      console.warn('Geen geldige token voor gamification notificatie stream.');
      return;
    }

    this.abortController = new AbortController();

    fetchEventSource(this.streamUrl, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      signal: this.abortController.signal,

      onopen: async (response) => {
        if (response.status === 401) {
          this.disconnect();
          this.authService.logout();
          return;
        }

        if (!response.ok) {
          this.disconnect();
          throw new Error(
            `Gamification notificatie stream kon niet openen. Status: ${response.status}`,
          );
        }

        console.log('Gamification notification stream connected');
      },

      onmessage: (event) => {
        if (event.event === 'connected') {
          return;
        }

        if (event.event !== 'gamification-notification') {
          return;
        }

        const notification = JSON.parse(event.data) as GamificationNotification;

        this.notificationStore.addNotification(notification);
      },

      onerror: (error) => {
        console.warn('Gamification notification stream error', error);

        this.disconnect();

        throw error;
      },
    });
  }

  public disconnect(): void {
    this.abortController?.abort();
    this.abortController = null;
  }
}

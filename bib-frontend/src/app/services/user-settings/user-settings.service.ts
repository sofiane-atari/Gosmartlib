import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const HOVER_HELP_STORAGE_KEY = 'hoverHelpEnabled';

@Injectable({
  providedIn: 'root'
})
export class UserSettingsService {
  private hoverHelpEnabledSubject = new BehaviorSubject<boolean>(
    localStorage.getItem(HOVER_HELP_STORAGE_KEY) !== 'false'
  );

  hoverHelpEnabled$ = this.hoverHelpEnabledSubject.asObservable();

  get hoverHelpEnabled(): boolean {
    return this.hoverHelpEnabledSubject.value;
  }

  setHoverHelpEnabled(enabled: boolean): void {
    localStorage.setItem(HOVER_HELP_STORAGE_KEY, String(enabled));
    this.hoverHelpEnabledSubject.next(enabled);
  }
}
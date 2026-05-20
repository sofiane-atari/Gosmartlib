import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const HOVER_HELP_STORAGE_KEY = 'hoverHelpEnabled';

@Injectable({
  providedIn: 'root'
})
export class UserSettingsService {
  private readonly catalogFeaturedListsEnabledKey = 'catalogFeaturedListsEnabled';
  private readonly catalogMostReadEnabledKey = 'catalogMostReadEnabled';
  private readonly catalogNewestEnabledKey = 'catalogNewestEnabled';
  
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


  public readonly catalogFeaturedListsEnabled$ = new BehaviorSubject<boolean>(
    this.getStoredBoolean(this.catalogFeaturedListsEnabledKey, true),
  );

  public readonly catalogMostReadEnabled$ = new BehaviorSubject<boolean>(
    this.getStoredBoolean(this.catalogMostReadEnabledKey, true),
  );

  public readonly catalogNewestEnabled$ = new BehaviorSubject<boolean>(
    this.getStoredBoolean(this.catalogNewestEnabledKey, true),
  );

  private getStoredBoolean(key: string, fallback: boolean): boolean {
    const storedValue = localStorage.getItem(key);

    if (storedValue === null) {
      return fallback;
    }

    return storedValue === 'true';
  }

  public setCatalogFeaturedListsEnabled(enabled: boolean): void {
    localStorage.setItem(this.catalogFeaturedListsEnabledKey, String(enabled));
    this.catalogFeaturedListsEnabled$.next(enabled);
  }

  public setCatalogMostReadEnabled(enabled: boolean): void {
    localStorage.setItem(this.catalogMostReadEnabledKey, String(enabled));
    this.catalogMostReadEnabled$.next(enabled);
  }

  public setCatalogNewestEnabled(enabled: boolean): void {
    localStorage.setItem(this.catalogNewestEnabledKey, String(enabled));
    this.catalogNewestEnabled$.next(enabled);
  }
}
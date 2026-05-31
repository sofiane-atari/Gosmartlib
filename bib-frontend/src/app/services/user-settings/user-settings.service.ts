import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../../../environments/environment';

const HOVER_HELP_STORAGE_KEY = 'hoverHelpEnabled';

@Injectable({
  providedIn: 'root'
})
export class UserSettingsService {
  private readonly http = inject(HttpClient);

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

  private readonly catalogFeaturedListsEnabledSubject = new BehaviorSubject<boolean>(true);
  private readonly catalogMostReadEnabledSubject = new BehaviorSubject<boolean>(true);
  private readonly catalogNewestEnabledSubject = new BehaviorSubject<boolean>(true);

  readonly catalogFeaturedListsEnabled$ = this.catalogFeaturedListsEnabledSubject.asObservable();
  readonly catalogMostReadEnabled$ = this.catalogMostReadEnabledSubject.asObservable();
  readonly catalogNewestEnabled$ = this.catalogNewestEnabledSubject.asObservable();

  loadCatalogSidebarSettings(): void {
    this.http.get<{
      catalogFeaturedListsEnabled: boolean;
      catalogMostReadEnabled: boolean;
      catalogNewestEnabled: boolean;
    }>(`${environment.apiUrl}/schools/me/catalog-sidebar-settings`).subscribe({
      next: (settings) => {
        this.catalogFeaturedListsEnabledSubject.next(settings.catalogFeaturedListsEnabled);
        this.catalogMostReadEnabledSubject.next(settings.catalogMostReadEnabled);
        this.catalogNewestEnabledSubject.next(settings.catalogNewestEnabled);
      },
    });
  }

  setCatalogFeaturedListsEnabled(enabled: boolean): void {
    this.catalogFeaturedListsEnabledSubject.next(enabled);
    this.persistCatalogSidebarSettings();
  }

  setCatalogMostReadEnabled(enabled: boolean): void {
    this.catalogMostReadEnabledSubject.next(enabled);
    this.persistCatalogSidebarSettings();
  }

  setCatalogNewestEnabled(enabled: boolean): void {
    this.catalogNewestEnabledSubject.next(enabled);
    this.persistCatalogSidebarSettings();
  }

  private persistCatalogSidebarSettings(): void {
    this.http.put<{
      catalogFeaturedListsEnabled: boolean;
      catalogMostReadEnabled: boolean;
      catalogNewestEnabled: boolean;
    }>(`${environment.apiUrl}/schools/me/catalog-sidebar-settings`, {
      catalogFeaturedListsEnabled: this.catalogFeaturedListsEnabledSubject.value,
      catalogMostReadEnabled: this.catalogMostReadEnabledSubject.value,
      catalogNewestEnabled: this.catalogNewestEnabledSubject.value,
    }).subscribe();
  }
}

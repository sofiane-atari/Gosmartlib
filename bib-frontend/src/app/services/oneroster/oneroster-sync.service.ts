import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SyncConfig } from '../../model/sync-config.model';

@Injectable({
  providedIn: 'root',
})
export class OneRosterSyncService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/oneroster`;

  public triggerSync(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/sync`, null);
  }

  public getConfig(): Observable<SyncConfig> {
    return this.http.get<SyncConfig>(`${this.baseUrl}/config`);
  }
}

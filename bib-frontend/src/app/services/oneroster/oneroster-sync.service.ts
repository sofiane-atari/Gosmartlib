import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class OneRosterSyncService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/oneroster/sync`;

  public triggerSync(): Observable<void> {
    return this.http.post<void>(this.url, null);
  }
}

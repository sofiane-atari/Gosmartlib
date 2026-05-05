import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { GamificationProfile } from '../../model/gamification/gamification.model';

@Injectable({
  providedIn: 'root',
})
export class GamificationService {
  private readonly apiUrl = `${environment.apiUrl}/gamification/me`;

  private readonly httpClient = inject(HttpClient);

  public getCurrentUserProfile(): Observable<GamificationProfile> {
    return this.httpClient.get<GamificationProfile>(this.apiUrl);
  }
}

import { Injectable, inject } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { AuthService } from './auth.service';

// wa de backend geeft
export interface ReadingList {
  id: number;
  name: string;
}

// wat je naar de backend stuurt
export interface CreateReadingListDTO {
  userId: number;
  name: string;
}


import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReadingListService {
  private apiUrl = `${environment.apiUrl}/reading-lists`;
  private authService = inject(AuthService);

  constructor(private http: HttpClient) { }

  createList(name: string): Observable<ReadingList> {
    const userId = this.authService.getCurrentUserId();
    if (userId === null) {
      throw new Error('Geen gebruiker ingelogd');
    }
    const dto: CreateReadingListDTO = { userId, name };
    return this.http.post<ReadingList>(this.apiUrl, dto);
  }

  getListsByUser(userId?: number): Observable<ReadingList[]> {
    const finalUserId = userId ?? this.authService.getCurrentUserId();
    if (finalUserId === null) {
      throw new Error('Geen gebruiker ingelogd');
    }
    return this.http.get<ReadingList[]>(`${this.apiUrl}/user/${finalUserId}`);
  }
}

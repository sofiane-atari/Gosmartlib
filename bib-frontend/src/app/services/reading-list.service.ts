import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";

// wat de backend geeft
export interface ReadingList {
  id: number;
  name: string;
}

// wat je naar de backend stuurt
export interface CreateReadingListDTO {
  name: string;
}

import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReadingListService {
  private apiUrl = `${environment.apiUrl}/reading-lists`;

  constructor(private http: HttpClient) { }

  createList(name: string): Observable<ReadingList> {
    const dto: CreateReadingListDTO = { name };
    return this.http.post<ReadingList>(this.apiUrl, dto);
  }

  getLists(): Observable<ReadingList[]> {
    return this.http.get<ReadingList[]>(this.apiUrl);
  }

  deleteList(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

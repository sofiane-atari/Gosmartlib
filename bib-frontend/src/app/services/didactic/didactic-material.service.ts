import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { from, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateDidacticMaterialRequest,
  DidacticMaterial,
} from '../../model/didactic/didactic-material.model';

@Injectable({
  providedIn: 'root',
})
export class DidacticMaterialService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/didactic-materials`;

  public getAll(): Observable<DidacticMaterial[]> {
    return this.http.get<DidacticMaterial[]>(this.baseUrl);
  }

  public getById(id: number): Observable<DidacticMaterial> {
    return this.http.get<DidacticMaterial>(`${this.baseUrl}/${id}`);
  }

  public create(
    request: CreateDidacticMaterialRequest,
  ): Observable<DidacticMaterial> {
    return this.http.post<DidacticMaterial>(this.baseUrl, request);
  }

  public update(
    id: number,
    request: CreateDidacticMaterialRequest,
  ): Observable<DidacticMaterial> {
    return this.http.put<DidacticMaterial>(`${this.baseUrl}/${id}`, request);
  }

  public delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
